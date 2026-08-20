package com.company.devvault.artifact.entity;

import com.company.devvault.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "artifacts", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "artifact_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", unique = true, length = 420)
    private String slug;

    @Column(name = "group_id", nullable = false, length = 200)
    private String groupId;

    @Column(name = "artifact_id", nullable = false, length = 200)
    private String artifactId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String packaging = "jar";

    @Column(nullable = false, length = 20)
    private String language = "java";

    @Column(name = "repository_type", nullable = false, length = 20)
    private String repositoryType = "maven";

    @Column(name = "readme", columnDefinition = "TEXT")
    private String readme;

    @Column(name = "git_url", length = 500)
    private String gitUrl;

    @Column(name = "license_name", length = 100)
    private String licenseName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ArtifactStatus status = ArtifactStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    private ArtifactSource source = ArtifactSource.INTERNAL;

    @Column(name = "download_count", nullable = false)
    private long downloadCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        ensureSlug();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
        ensureSlug();
    }

    private void ensureSlug() {
        if (this.slug == null || this.slug.isBlank()) {
            this.slug = this.groupId + ":" + this.artifactId;
        }
    }
}