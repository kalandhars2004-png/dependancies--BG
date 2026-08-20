package com.company.devvault.artifact.service;

import com.company.devvault.analytics.entity.DownloadEvent;
import com.company.devvault.analytics.repository.DownloadEventRepository;
import com.company.devvault.artifact.dto.ArtifactRequest;
import com.company.devvault.artifact.dto.ArtifactResponse;
import com.company.devvault.artifact.dto.VersionRequest;
import com.company.devvault.artifact.dto.VersionResponse;
import com.company.devvault.artifact.entity.Artifact;
import com.company.devvault.artifact.entity.ArtifactFile;
import com.company.devvault.artifact.entity.ArtifactFileType;
import com.company.devvault.artifact.entity.ArtifactMaintainer;
import com.company.devvault.artifact.entity.ArtifactTag;
import com.company.devvault.artifact.entity.ArtifactVersion;
import com.company.devvault.artifact.entity.Category;
import com.company.devvault.artifact.entity.Tag;
import com.company.devvault.artifact.mapper.ArtifactMapper;
import com.company.devvault.artifact.repository.ArtifactFileRepository;
import com.company.devvault.artifact.repository.ArtifactMaintainerRepository;
import com.company.devvault.artifact.repository.ArtifactRepository;
import com.company.devvault.artifact.repository.ArtifactTagRepository;
import com.company.devvault.artifact.repository.ArtifactVersionRepository;
import com.company.devvault.artifact.repository.CategoryRepository;
import com.company.devvault.artifact.repository.TagRepository;
import com.company.devvault.audit.service.AuditService;
import com.company.devvault.auth.entity.User;
import com.company.devvault.auth.entity.UserRole;
import com.company.devvault.auth.repository.UserRepository;
import com.company.devvault.common.exception.ApiException;
import com.company.devvault.common.util.ChecksumUtil;
import com.company.devvault.common.util.MavenCoordinates;
import com.company.devvault.storage.ArtifactStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final ArtifactVersionRepository versionRepository;
    private final ArtifactFileRepository fileRepository;
    private final ArtifactTagRepository artifactTagRepository;
    private final ArtifactMaintainerRepository maintainerRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DownloadEventRepository downloadEventRepository;
    private final ArtifactStorageService storageService;
    private final AuditService auditService;
    private final ArtifactMapper artifactMapper;

    public ArtifactService(ArtifactRepository artifactRepository,
                           ArtifactVersionRepository versionRepository,
                           ArtifactFileRepository fileRepository,
                           ArtifactTagRepository artifactTagRepository,
                           ArtifactMaintainerRepository maintainerRepository,
                           TagRepository tagRepository,
                           CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           DownloadEventRepository downloadEventRepository,
                           ArtifactStorageService storageService,
                           AuditService auditService,
                           ArtifactMapper artifactMapper) {
        this.artifactRepository = artifactRepository;
        this.versionRepository = versionRepository;
        this.fileRepository = fileRepository;
        this.artifactTagRepository = artifactTagRepository;
        this.maintainerRepository = maintainerRepository;
        this.tagRepository = tagRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.downloadEventRepository = downloadEventRepository;
        this.storageService = storageService;
        this.auditService = auditService;
        this.artifactMapper = artifactMapper;
    }

    // ---------- Artifact CRUD ----------

    @Transactional
    public ArtifactResponse createArtifact(ArtifactRequest request, User owner) {
        MavenCoordinates.validate(request.getGroupId(), request.getArtifactId(), "0.0.0");
        String groupId = request.getGroupId().trim();
        String artifactId = request.getArtifactId().trim();
        if (artifactRepository.existsByGroupIdAndArtifactId(groupId, artifactId)) {
            throw ApiException.conflict("Artifact " + groupId + ":" + artifactId + " already exists");
        }
        Artifact artifact = new Artifact();
        artifact.setGroupId(groupId);
        artifact.setArtifactId(artifactId);
        artifact.setName(request.getName().trim());
        artifact.setDescription(request.getDescription());
        artifact.setPackaging("jar");
        artifact.setLanguage(request.getLanguage() == null ? "java" : request.getLanguage());
        artifact.setReadme(request.getReadme());
        artifact.setGitUrl(request.getGitUrl());
        artifact.setLicenseName(request.getLicenseName());
        artifact.setCategory(resolveCategory(request.getCategoryName()));
        artifact.setOwner(owner);
        artifact = artifactRepository.save(artifact);

        applyTags(artifact, request.getTags());
        ArtifactMaintainer maintainer = new ArtifactMaintainer();
        maintainer.setArtifact(artifact);
        maintainer.setUser(owner);
        maintainerRepository.save(maintainer);

        auditService.log("ARTIFACT_CREATED", owner, "Artifact",
                groupId + ":" + artifactId, null);
        return toResponse(artifact);
    }

    @Transactional
    public ArtifactResponse updateArtifact(Long artifactId, ArtifactRequest request, User actor) {
        Artifact artifact = getArtifactEntity(artifactId);
        assertCanManage(artifact, actor);
        artifact.setName(request.getName().trim());
        artifact.setDescription(request.getDescription());
        artifact.setReadme(request.getReadme());
        artifact.setGitUrl(request.getGitUrl());
        artifact.setLicenseName(request.getLicenseName());
        artifact.setCategory(resolveCategory(request.getCategoryName()));
        artifactRepository.save(artifact);
        artifactTagRepository.deleteByArtifactId(artifact.getId());
        applyTags(artifact, request.getTags());
        auditService.log("ARTIFACT_UPDATED", actor, "Artifact",
                artifact.getGroupId() + ":" + artifact.getArtifactId(), null);
        return toResponse(artifact);
    }

    @Transactional
    public void deleteArtifact(Long artifactId, User actor) {
        if (actor.getRole() != UserRole.ADMIN) {
            throw ApiException.forbidden("Only administrators can delete artifacts");
        }
        Artifact artifact = getArtifactEntity(artifactId);
        downloadEventRepository.deleteByArtifactId(artifactId);
        List<ArtifactVersion> versions = versionRepository.findByArtifactIdOrderByCreatedAtDesc(artifactId);
        for (ArtifactVersion version : versions) {
            fileRepository.deleteByArtifactVersionId(version.getId());
            deleteVersionFiles(version);
        }
        versionRepository.deleteByArtifactId(artifactId);
        artifactTagRepository.deleteByArtifactId(artifactId);
        maintainerRepository.deleteAll(maintainerRepository.findByArtifactId(artifactId));
        artifactRepository.delete(artifact);
        auditService.log("ARTIFACT_DELETED", actor, "Artifact",
                artifact.getGroupId() + ":" + artifact.getArtifactId(), null);
    }

    // ---------- Reads ----------

    @Transactional(readOnly = true)
    public ArtifactResponse getArtifact(Long artifactId) {
        return toResponse(getArtifactEntity(artifactId));
    }

    @Transactional(readOnly = true)
    public ArtifactResponse getArtifact(String groupId, String artifactId) {
        Artifact artifact = artifactRepository.findByGroupIdAndArtifactId(groupId, artifactId)
                .orElseThrow(() -> ApiException.notFound("Artifact " + groupId + ":" + artifactId + " not found"));
        return toResponse(artifact);
    }

    @Transactional(readOnly = true)
    public ArtifactResponse getArtifactBySlug(String slug) {
        Artifact artifact = artifactRepository.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Artifact " + slug + " not found"));
        return toResponse(artifact);
    }

    @Transactional(readOnly = true)
    public Page<ArtifactResponse> listArtifacts(int page, int size, String sort, Long categoryId, Long ownerId) {
        Pageable pageable;
        if ("downloads".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "downloadCount"));
        } else {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Page<Artifact> entities;
        if (categoryId != null) {
            entities = artifactRepository.findByCategoryId(categoryId, pageable);
        } else if (ownerId != null) {
            entities = artifactRepository.findByOwnerId(ownerId, pageable);
        } else {
            entities = artifactRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return entities.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ArtifactResponse> recentArtifacts(int limit) {
        return artifactRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .limit(limit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArtifactResponse> mostDownloaded(int limit) {
        return artifactRepository.findTop5ByOrderByDownloadCountDesc().stream()
                .limit(limit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ---------- Versions ----------

    @Transactional(readOnly = true)
    public List<VersionResponse> listVersions(Long artifactId) {
        Artifact artifact = getArtifactEntity(artifactId);
        return versionRepository.findByArtifactIdOrderByCreatedAtDesc(artifactId).stream()
                .map(v -> toVersionResponse(v, artifact))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VersionResponse getVersion(Long artifactId, String version) {
        Artifact artifact = getArtifactEntity(artifactId);
        ArtifactVersion entity = versionRepository.findByArtifactIdAndVersion(artifactId, version)
                .orElseThrow(() -> ApiException.notFound(
                        "Version " + version + " not found for " + artifact.getGroupId() + ":" + artifact.getArtifactId()));
        return toVersionResponse(entity, artifact);
    }

    @Transactional
    public VersionResponse publishVersion(Long artifactId, VersionRequest request, User actor,
                                          MultipartFile jar, MultipartFile pom,
                                          MultipartFile sources, MultipartFile javadoc) {
        Artifact artifact = getArtifactEntity(artifactId);
        assertCanPublish(artifact, actor);
        String version = request.getVersion().trim();
        MavenCoordinates.validateVersion(version);
        if (versionRepository.existsByArtifactIdAndVersion(artifactId, version)) {
            throw ApiException.conflict("Version " + version + " already exists. Versions are immutable.");
        }
        if (jar == null || jar.isEmpty()) {
            throw ApiException.badRequest("The JAR file is required for publishing");
        }

        ArtifactVersion entity = new ArtifactVersion();
        entity.setArtifact(artifact);
        entity.setVersion(version);
        entity.setReleaseNotes(request.getReleaseNotes());
        entity.setPublishedBy(actor);
        entity = versionRepository.save(entity);

        storeFile(entity, ArtifactFileType.JAR, jar);
        if (pom != null && !pom.isEmpty()) {
            storeFile(entity, ArtifactFileType.POM, pom);
        }
        if (sources != null && !sources.isEmpty()) {
            storeFile(entity, ArtifactFileType.SOURCES, sources);
        }
        if (javadoc != null && !javadoc.isEmpty()) {
            storeFile(entity, ArtifactFileType.JAVADOC, javadoc);
        }
        artifact.setUpdatedAt(Instant.now());
        artifactRepository.save(artifact);

        auditService.log("VERSION_PUBLISHED", actor, "ArtifactVersion",
                artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + version, null);
        return toVersionResponse(entity, artifact);
    }

    @Transactional
    public VersionResponse markRecommended(Long artifactId, String version, User actor) {
        Artifact artifact = getArtifactEntity(artifactId);
        assertCanManage(artifact, actor);
        ArtifactVersion entity = versionRepository.findByArtifactIdAndVersion(artifactId, version)
                .orElseThrow(() -> ApiException.notFound("Version not found: " + version));
        if (entity.isDeprecated()) {
            throw ApiException.badRequest("Cannot recommend a deprecated version");
        }
        List<ArtifactVersion> all = versionRepository.findByArtifactIdOrderByCreatedAtDesc(artifactId);
        for (ArtifactVersion v : all) {
            v.setRecommended(v.getId().equals(entity.getId()));
        }
        versionRepository.saveAll(all);
        auditService.log("VERSION_RECOMMENDED", actor, "ArtifactVersion",
                artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + version, null);
        return toVersionResponse(entity, artifact);
    }

    @Transactional
    public VersionResponse setDeprecated(Long artifactId, String version, boolean deprecated, User actor) {
        Artifact artifact = getArtifactEntity(artifactId);
        assertCanManage(artifact, actor);
        ArtifactVersion entity = versionRepository.findByArtifactIdAndVersion(artifactId, version)
                .orElseThrow(() -> ApiException.notFound("Version not found: " + version));
        entity.setDeprecated(deprecated);
        if (deprecated && entity.isRecommended()) {
            entity.setRecommended(false);
        }
        versionRepository.save(entity);
        auditService.log(deprecated ? "VERSION_DEPRECATED" : "VERSION_UN_DEPRECATED", actor, "ArtifactVersion",
                artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + version, null);
        return toVersionResponse(entity, artifact);
    }

    // ---------- File download (web API) ----------

    @Transactional
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            Long artifactId, String version, Long fileId, User user, String ip, String userAgent) {
        Artifact artifact = getArtifactEntity(artifactId);
        ArtifactVersion entity = versionRepository.findByArtifactIdAndVersion(artifactId, version)
                .orElseThrow(() -> ApiException.notFound("Version not found: " + version));
        ArtifactFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> ApiException.notFound("File not found"));
        if (!file.getArtifactVersion().getId().equals(entity.getId())) {
            throw ApiException.notFound("File does not belong to this version");
        }
        Path path = storageService.resolvePath(file.getStoragePath());
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(path);
        recordDownload(artifact, entity, file, user, ip, userAgent);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentLength(file.getFileSize())
                .contentType(MediaType.parseMediaType(contentType(file.getFileName())))
                .body(resource);
    }

    // ---------- Internal helpers ----------

    private Artifact getArtifactEntity(Long id) {
        return artifactRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Artifact not found with id " + id));
    }

    private void storeFile(ArtifactVersion version, ArtifactFileType type, MultipartFile upload) {
        Artifact artifact = version.getArtifact();
        String fileName = buildFileName(artifact.getArtifactId(), version.getVersion(), type);
        String relativePath = MavenCoordinates.toPath(artifact.getGroupId(), artifact.getArtifactId())
                + "/" + version.getVersion() + "/" + fileName;
        try (InputStream input = upload.getInputStream()) {
            storageService.storeArtifact(relativePath, input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded file " + fileName, e);
        }
        ArtifactFile file = new ArtifactFile();
        file.setArtifactVersion(version);
        file.setFileName(fileName);
        file.setFileType(type);
        file.setStoragePath(relativePath);
        file.setFileSize(upload.getSize());
        try (InputStream input = Files.newInputStream(storageService.resolvePath(relativePath))) {
            file.setChecksum(ChecksumUtil.sha256(input));
        } catch (IOException e) {
            throw new RuntimeException("Failed to compute checksum for " + fileName, e);
        }
        fileRepository.save(file);
        version.getFiles().add(file);
    }

    private String buildFileName(String artifactId, String version, ArtifactFileType type) {
        switch (type) {
            case POM:
                return artifactId + "-" + version + ".pom";
            case SOURCES:
                return artifactId + "-" + version + "-sources.jar";
            case JAVADOC:
                return artifactId + "-" + version + "-javadoc.jar";
            default:
                return artifactId + "-" + version + ".jar";
        }
    }

    private void deleteVersionFiles(ArtifactVersion version) {
        for (ArtifactFile file : fileRepository.findByArtifactVersionId(version.getId())) {
            storageService.deleteArtifact(file.getStoragePath());
        }
    }

    private Category resolveCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        String name = categoryName.trim();
        return categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setSlug(name.toLowerCase(Locale.ROOT).replace(' ', '-'));
            return categoryRepository.save(category);
        });
    }

    private void applyTags(Artifact artifact, List<String> tagNames) {
        if (tagNames == null) {
            return;
        }
        for (String raw : tagNames) {
            String name = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
            if (name.isEmpty()) {
                continue;
            }
            Tag tag = tagRepository.findByName(name).orElseGet(() -> tagRepository.save(new Tag(null, name)));
            ArtifactTag artifactTag = new ArtifactTag();
            artifactTag.setArtifact(artifact);
            artifactTag.setTag(tag);
            artifactTagRepository.save(artifactTag);
        }
    }

    private void assertCanManage(Artifact artifact, User actor) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        boolean isMaintainer = maintainerRepository.existsByArtifactIdAndUserId(artifact.getId(), actor.getId());
        if (!isMaintainer) {
            throw ApiException.forbidden("You are not a maintainer of this artifact");
        }
    }

    private void assertCanPublish(Artifact artifact, User actor) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.MAINTAINER) {
            return;
        }
        boolean isMaintainer = maintainerRepository.existsByArtifactIdAndUserId(artifact.getId(), actor.getId());
        if (!isMaintainer) {
            throw ApiException.forbidden("Only maintainers can publish versions to this artifact");
        }
    }

    private ArtifactResponse toResponse(Artifact artifact) {
        List<String> tags = artifactTagRepository.findByArtifactId(artifact.getId()).stream()
                .map(at -> at.getTag().getName())
                .collect(Collectors.toList());
        List<String> maintainers = maintainerRepository.findByArtifactId(artifact.getId()).stream()
                .map(m -> m.getUser().getName())
                .collect(Collectors.toList());
        String latest = versionRepository.findFirstByArtifactIdOrderByCreatedAtDesc(artifact.getId())
                .map(ArtifactVersion::getVersion).orElse(null);
        String recommended = versionRepository.findFirstByArtifactIdAndRecommendedTrueOrderByCreatedAtDesc(artifact.getId())
                .map(ArtifactVersion::getVersion).orElse(null);
        long versionCount = versionRepository.countByArtifactId(artifact.getId());
        return artifactMapper.toResponse(artifact, latest, recommended, tags, maintainers, versionCount);
    }

    private VersionResponse toVersionResponse(ArtifactVersion version, Artifact artifact) {
        List<VersionResponse.FileInfo> files = fileRepository.findByArtifactVersionId(version.getId()).stream()
                .map(artifactMapper::toFileInfo)
                .collect(Collectors.toList());
        String publisher = version.getPublishedBy() != null ? version.getPublishedBy().getName() : null;
        return artifactMapper.toVersionResponse(version, publisher, files);
    }

    private void recordDownload(Artifact artifact, ArtifactVersion version, ArtifactFile file,
                                User user, String ip, String userAgent) {
        artifact.setDownloadCount(artifact.getDownloadCount() + 1);
        version.setDownloadCount(version.getDownloadCount() + 1);
        artifactRepository.save(artifact);
        versionRepository.save(version);
        DownloadEvent event = new DownloadEvent();
        event.setArtifact(artifact);
        event.setArtifactVersion(version);
        event.setArtifactFile(file);
        event.setUser(user);
        event.setIpAddress(ip);
        event.setUserAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent);
        downloadEventRepository.save(event);
        if (user != null) {
            auditService.log("ARTIFACT_DOWNLOADED", user, "ArtifactFile",
                    artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + version.getVersion(), null);
        }
    }

    private String contentType(String fileName) {
        if (fileName.endsWith(".jar")) {
            return "application/java-archive";
        }
        return "application/octet-stream";
    }
}