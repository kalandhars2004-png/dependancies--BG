package com.company.devvault.artifact.repository;

import com.company.devvault.artifact.entity.ArtifactTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtifactTagRepository extends JpaRepository<ArtifactTag, Long> {

    List<ArtifactTag> findByArtifactId(Long artifactId);

    void deleteByArtifactId(Long artifactId);
}