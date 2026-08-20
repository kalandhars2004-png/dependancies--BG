package com.company.devvault.artifact.repository;

import com.company.devvault.artifact.entity.ArtifactVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ArtifactVersionRepository extends JpaRepository<ArtifactVersion, Long> {

    List<ArtifactVersion> findByArtifactIdOrderByCreatedAtDesc(Long artifactId);

    Optional<ArtifactVersion> findByArtifactIdAndVersion(Long artifactId, String version);

    boolean existsByArtifactIdAndVersion(Long artifactId, String version);

    Optional<ArtifactVersion> findFirstByArtifactIdOrderByCreatedAtDesc(Long artifactId);

    Optional<ArtifactVersion> findFirstByArtifactIdAndRecommendedTrueOrderByCreatedAtDesc(Long artifactId);

    long countByCreatedAtAfter(Instant after);

    void deleteByArtifactId(Long artifactId);

    long countByArtifactId(Long artifactId);

    long count();
}