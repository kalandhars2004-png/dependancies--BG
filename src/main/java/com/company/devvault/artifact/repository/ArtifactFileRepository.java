package com.company.devvault.artifact.repository;

import com.company.devvault.artifact.entity.ArtifactFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtifactFileRepository extends JpaRepository<ArtifactFile, Long> {

    List<ArtifactFile> findByArtifactVersionId(Long artifactVersionId);

    void deleteByArtifactVersionId(Long artifactVersionId);

    long count();
}