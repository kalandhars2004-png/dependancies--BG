package com.company.devvault.config;

import com.company.devvault.artifact.entity.Artifact;
import com.company.devvault.artifact.entity.ArtifactFile;
import com.company.devvault.artifact.entity.ArtifactFileType;
import com.company.devvault.artifact.entity.ArtifactMaintainer;
import com.company.devvault.artifact.entity.ArtifactTag;
import com.company.devvault.artifact.entity.ArtifactVersion;
import com.company.devvault.artifact.entity.Category;
import com.company.devvault.artifact.entity.Tag;
import com.company.devvault.artifact.repository.ArtifactFileRepository;
import com.company.devvault.artifact.repository.ArtifactMaintainerRepository;
import com.company.devvault.artifact.repository.ArtifactRepository;
import com.company.devvault.artifact.repository.ArtifactTagRepository;
import com.company.devvault.artifact.repository.ArtifactVersionRepository;
import com.company.devvault.artifact.repository.CategoryRepository;
import com.company.devvault.artifact.repository.TagRepository;
import com.company.devvault.auth.entity.User;
import com.company.devvault.auth.entity.UserRole;
import com.company.devvault.auth.repository.UserRepository;
import com.company.devvault.common.util.ChecksumUtil;
import com.company.devvault.storage.ArtifactStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArtifactRepository artifactRepository;
    private final ArtifactVersionRepository versionRepository;
    private final ArtifactFileRepository fileRepository;
    private final ArtifactTagRepository artifactTagRepository;
    private final ArtifactMaintainerRepository maintainerRepository;
    private final ArtifactStorageService storageService;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                      TagRepository tagRepository, ArtifactRepository artifactRepository,
                      ArtifactVersionRepository versionRepository, ArtifactFileRepository fileRepository,
                      ArtifactTagRepository artifactTagRepository, ArtifactMaintainerRepository maintainerRepository,
                      ArtifactStorageService storageService) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.artifactRepository = artifactRepository;
        this.versionRepository = versionRepository;
        this.fileRepository = fileRepository;
        this.artifactTagRepository = artifactTagRepository;
        this.maintainerRepository = maintainerRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Data seeding skipped - users already exist");
            return;
        }
        log.info("Seeding DevVault sample data...");

        User admin = createUser("Kalandhar", "admin@devvault.local", "Admin@123", UserRole.ADMIN, "Engineering");

        Category commonUtils = category("Common Utilities");
        Category security = category("Security");
        Category payment = category("Payment");

        tag("logging");
        tag("security");
        tag("jwt");
        tag("date");
        tag("payment");
        tag("client");
        tag("spring");
        tag("common");

        seedArtifact("com.company.common", "logging-utils", "Logging Utils",
                "Common logging utilities used across company Spring Boot applications.",
                "Common utilities for structured logging, log masking and redaction.",
                commonUtils, List.of("logging", "common"), admin,
                "https://github.com/company/logging-utils", "Apache-2.0",
                List.of(new SeedVersion("1.0.0", admin, "Initial release with format() and masked() helpers.", true, false)),
                "logging-utils-1.0.0.jar");

        seedArtifact("com.company.common", "security-utils", "Security Utils",
                "Security helpers for JWT handling, password strength and header sanitization.",
                "Reusable security utilities including password policy validation and safe header handling.",
                security, List.of("security", "jwt", "spring", "common"), admin,
                "https://github.com/company/security-utils", "Apache-2.0",
                List.of(
                        new SeedVersion("1.0.0", admin, "Initial release.", false, true),
                        new SeedVersion("1.1.0", admin, "Added password strength validation.", false, false),
                        new SeedVersion("1.2.0", admin, "Added header sanitization. Recommended version.", true, false)
                ),
                "security-utils-1.2.0.jar",
                "security-utils-1.1.0.jar",
                "security-utils-1.0.0.jar");

        seedArtifact("com.company.common", "date-utils", "Date Utils",
                "Date and time formatting helpers shared by company services.",
                "Simple utilities for formatting dates across applications.",
                commonUtils, List.of("date", "common"), admin,
                "https://github.com/company/date-utils", "MIT",
                List.of(new SeedVersion("2.0.0", admin, "Rewritten for java.time API.", true, false)),
                "date-utils-2.0.0.jar");

        seedArtifact("com.company.payment", "payment-client", "Payment Client",
                "Java client for the company payment gateway.",
                "Thin HTTP client wrapping the internal payment gateway REST API.",
                payment, List.of("payment", "client"), admin,
                "https://github.com/company/payment-client", "Proprietary",
                List.of(new SeedVersion("1.1.0", admin, "Added ping() health check.", true, false)),
                "payment-client-1.1.0.jar");

        log.info("Sample data seeded. Account:");
        log.info("  admin@devvault.local / Admin@123  (ADMIN)");
    }

    private User createUser(String name, String email, String password, UserRole role, String department) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password));
        user.setRole(role);
        user.setDepartment(department);
        return userRepository.save(user);
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(name.toLowerCase(Locale.ROOT).replace(' ', '-'));
        return categoryRepository.save(category);
    }

    private void tag(String name) {
        tagRepository.save(new Tag(null, name));
    }

    private void seedArtifact(String groupId, String artifactId, String name, String description, String readme,
                              Category category, List<String> tagNames, User owner, String gitUrl, String license,
                              List<SeedVersion> versions, String... seedJarNames) {
        Artifact artifact = new Artifact();
        artifact.setGroupId(groupId);
        artifact.setArtifactId(artifactId);
        artifact.setName(name);
        artifact.setDescription(description);
        artifact.setReadme(readme);
        artifact.setCategory(category);
        artifact.setOwner(owner);
        artifact.setGitUrl(gitUrl);
        artifact.setLicenseName(license);
        artifact = artifactRepository.save(artifact);

        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName).orElseGet(() -> tagRepository.save(new Tag(null, tagName)));
            ArtifactTag artifactTag = new ArtifactTag();
            artifactTag.setArtifact(artifact);
            artifactTag.setTag(tag);
            artifactTagRepository.save(artifactTag);
        }

        ArtifactMaintainer maintainer = new ArtifactMaintainer();
        maintainer.setArtifact(artifact);
        maintainer.setUser(owner);
        maintainerRepository.save(maintainer);

        for (int i = 0; i < versions.size(); i++) {
            SeedVersion sv = versions.get(i);
            ArtifactVersion version = new ArtifactVersion();
            version.setArtifact(artifact);
            version.setVersion(sv.version);
            version.setReleaseNotes(sv.releaseNotes);
            version.setRecommended(sv.recommended);
            version.setDeprecated(sv.deprecated);
            version.setPublishedBy(sv.publisher);
            version = versionRepository.save(version);

            String seedJar = seedJarNames[i];
            storeSeedJar(artifact, version, seedJar);
            storeGeneratedPom(artifact, version, name, description, license);
        }
    }

    private void storeSeedJar(Artifact artifact, ArtifactVersion version, String seedJarName) {
        String fileName = artifact.getArtifactId() + "-" + version.getVersion() + ".jar";
        String relativePath = pathOf(artifact) + "/" + version.getVersion() + "/" + fileName;
        try (InputStream input = new ClassPathResource("seed-artifacts/" + seedJarName).getInputStream()) {
            storageService.storeArtifact(relativePath, input);
            saveFileRow(artifact, version, fileName, ArtifactFileType.JAR, relativePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to seed jar " + seedJarName, e);
        }
    }

    private void storeGeneratedPom(Artifact artifact, ArtifactVersion version, String name, String description, String license) {
        String fileName = artifact.getArtifactId() + "-" + version.getVersion() + ".pom";
        String relativePath = pathOf(artifact) + "/" + version.getVersion() + "/" + fileName;
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>" + artifact.getGroupId() + "</groupId>\n" +
                "  <artifactId>" + artifact.getArtifactId() + "</artifactId>\n" +
                "  <version>" + version.getVersion() + "</version>\n" +
                "  <packaging>jar</packaging>\n" +
                "  <name>" + name + "</name>\n" +
                "  <description>" + description + "</description>\n" +
                "  <licenses><license><name>" + license + "</name></license></licenses>\n" +
                "</project>\n";
        storageService.storeArtifact(relativePath, new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));
        saveFileRow(artifact, version, fileName, ArtifactFileType.POM, relativePath);
    }

    private void saveFileRow(Artifact artifact, ArtifactVersion version, String fileName,
                             ArtifactFileType type, String relativePath) {
        ArtifactFile file = new ArtifactFile();
        file.setArtifactVersion(version);
        file.setFileName(fileName);
        file.setFileType(type);
        file.setStoragePath(relativePath);
        Path path = storageService.resolvePath(relativePath);
        try {
            file.setFileSize(Files.size(path));
            try (InputStream input = Files.newInputStream(path)) {
                file.setChecksum(ChecksumUtil.sha256(input));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read seeded file " + fileName, e);
        }
        fileRepository.save(file);
    }

    private String pathOf(Artifact artifact) {
        return artifact.getGroupId().replace('.', '/') + "/" + artifact.getArtifactId();
    }

    private static class SeedVersion {
        final String version;
        final User publisher;
        final String releaseNotes;
        final boolean recommended;
        final boolean deprecated;

        SeedVersion(String version, User publisher, String releaseNotes, boolean recommended, boolean deprecated) {
            this.version = version;
            this.publisher = publisher;
            this.releaseNotes = releaseNotes;
            this.recommended = recommended;
            this.deprecated = deprecated;
        }
    }
}