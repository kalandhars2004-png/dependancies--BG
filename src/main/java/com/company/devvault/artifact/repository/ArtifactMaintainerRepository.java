package com.company.devvault.artifact.repository;

import com.company.devvault.artifact.entity.ArtifactMaintainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtifactMaintainerRepository extends JpaRepository<ArtifactMaintainer, Long> {

    List<ArtifactMaintainer> findByArtifactId(Long artifactId);

    boolean existsByArtifactIdAndUserId(Long artifactId, Long userId);
}