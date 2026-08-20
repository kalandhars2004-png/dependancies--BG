package com.company.devvault.maven.service;

import com.company.devvault.artifact.entity.Artifact;
import com.company.devvault.artifact.entity.ArtifactFile;
import com.company.devvault.artifact.entity.ArtifactFileType;
import com.company.devvault.artifact.entity.ArtifactSource;
import com.company.devvault.artifact.entity.ArtifactVersion;
import com.company.devvault.artifact.repository.ArtifactFileRepository;
import com.company.devvault.artifact.repository.ArtifactRepository;
import com.company.devvault.artifact.repository.ArtifactVersionRepository;
import com.company.devvault.audit.service.AuditService;
import com.company.devvault.auth.entity.User;
import com.company.devvault.auth.entity.UserRole;
import com.company.devvault.auth.repository.UserRepository;
import com.company.devvault.common.util.ChecksumUtil;
import com.company.devvault.storage.ArtifactStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Fetches dependencies that are missing from the local registry from a remote
 * Maven repository (Maven Central by default), stores them in the local
 * database + file storage, and makes them browsable on the site.
 */
@Service
public class RemoteMavenProxyService {

    private static final Logger log = LoggerFactory.getLogger(RemoteMavenProxyService.class);
    private static final String PROXY_EMAIL = "maven-proxy@devvault.local";

    private final boolean enabled;
    private final String remoteUrl;
    private final HttpClient httpClient;
    private final ArtifactRepository artifactRepository;
    private final ArtifactVersionRepository versionRepository;
    private final ArtifactFileRepository fileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArtifactStorageService storageService;
    private final AuditService auditService;

    public RemoteMavenProxyService(
            @Value("${devvault.remote.enabled:true}") boolean enabled,
            @Value("${devvault.remote.url:https://repo1.maven.org/maven2}") String remoteUrl,
            @Value("${devvault.remote.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${devvault.remote.read-timeout-ms:30000}") long readTimeoutMs,
            ArtifactRepository artifactRepository,
            ArtifactVersionRepository versionRepository,
            ArtifactFileRepository fileRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ArtifactStorageService storageService,
            AuditService auditService) {
        this.enabled = enabled;
        this.remoteUrl = remoteUrl.replaceAll("/+$", "");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.artifactRepository = artifactRepository;
        this.versionRepository = versionRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageService = storageService;
        this.auditService = auditService;
    }

    public boolean enabled() {
        return enabled;
    }

    /**
     * Fetches a file from the remote repository. Returns null if the remote
     * file does not exist or the fetch fails (so callers can fall back to 404).
     */
    public byte[] fetch(String relativePath) {
        if (!enabled) {
            return null;
        }
        String url = remoteUrl + "/" + relativePath;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(30000))
                    .header("User-Agent", "DevVault/1.0 (Maven proxy)")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            }
            if (response.statusCode() != 404) {
                log.warn("Remote proxy {} returned HTTP {}", url, response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Remote proxy fetch failed for {}: {}", url, e.getMessage());
        }
        return null;
    }

    /**
     * Downloads a missing artifact file from the remote repository, persists it
     * in the local database (artifact + version + file rows) and on disk, and
     * returns the stored file entity. Safe to call concurrently - if another
     * request imported the same file first, the existing row is returned.
     */
    @Transactional
    public ArtifactFile importFile(String groupId, String artifactId, String version,
                                   String fileName, ArtifactFileType fileType, byte[] bytes) {
        User proxyUser = proxyUser();
        Artifact artifact = artifactRepository.findByGroupIdAndArtifactId(groupId, artifactId)
                .orElseGet(() -> {
                    Artifact created = new Artifact();
                    created.setGroupId(groupId);
                    created.setArtifactId(artifactId);
                    created.setName(humanize(artifactId));
                    created.setDescription("Proxied automatically from Maven Central");
                    created.setLicenseName("See pom for license details");
                    created.setOwner(proxyUser);
                    created.setSource(ArtifactSource.PROXY);
                    Artifact saved = artifactRepository.save(created);
                    auditService.log("ARTIFACT_PROXIED", proxyUser, "Artifact",
                            groupId + ":" + artifactId, "imported from " + remoteUrl);
                    return saved;
                });

        ArtifactVersion entity = versionRepository.findByArtifactIdAndVersion(artifact.getId(), version)
                .orElseGet(() -> {
                    ArtifactVersion created = new ArtifactVersion();
                    created.setArtifact(artifact);
                    created.setVersion(version);
                    created.setReleaseNotes("Proxied automatically from Maven Central");
                    created.setPublishedBy(proxyUser);
                    created.setCreatedAt(Instant.now());
                    return versionRepository.save(created);
                });

        return fileRepository.findByArtifactVersionId(entity.getId()).stream()
                .filter(f -> f.getFileType() == fileType)
                .findFirst()
                .orElseGet(() -> {
                    String relativePath = groupId.replace('.', '/') + "/" + artifactId
                            + "/" + version + "/" + fileName;
                    try (InputStream input = new ByteArrayInputStream(bytes)) {
                        storageService.storeArtifact(relativePath, input);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to store proxied file " + fileName, e);
                    }
                    ArtifactFile file = new ArtifactFile();
                    file.setArtifactVersion(entity);
                    file.setFileName(fileName);
                    file.setFileType(fileType);
                    file.setStoragePath(relativePath);
                    file.setFileSize(bytes.length);
                    try (InputStream input = Files.newInputStream(storageService.resolvePath(relativePath))) {
                        file.setChecksum(ChecksumUtil.sha256(input));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to checksum proxied file " + fileName, e);
                    }
                    return fileRepository.save(file);
                });
    }

    private User proxyUser() {
        return userRepository.findByEmail(PROXY_EMAIL).orElseGet(() -> {
            User user = new User();
            user.setEmail(PROXY_EMAIL);
            user.setName("Maven Central Proxy");
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole(UserRole.DEVELOPER);
            return userRepository.save(user);
        });
    }

    private String humanize(String artifactId) {
        String readable = artifactId.replace('-', ' ').replace('_', ' ');
        String[] words = readable.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return builder.length() > 0 ? builder.toString() : artifactId;
    }
}