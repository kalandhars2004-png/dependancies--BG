package com.company.devvault.artifact.repository;

import com.company.devvault.artifact.entity.Artifact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    Optional<Artifact> findByGroupIdAndArtifactId(String groupId, String artifactId);

    Optional<Artifact> findBySlug(String slug);

    boolean existsByGroupIdAndArtifactId(String groupId, String artifactId);

    Page<Artifact> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Artifact> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Artifact> findByOwnerId(Long ownerId, Pageable pageable);

    List<Artifact> findTop10ByOrderByCreatedAtDesc();

    List<Artifact> findTop5ByOrderByDownloadCountDesc();

    @Query("SELECT a FROM Artifact a WHERE LOWER(a.artifactId) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(a.groupId) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Artifact> search(@Param("q") String q);

    @Query("SELECT COALESCE(SUM(a.downloadCount), 0) FROM Artifact a")
    long sumDownloadCount();
}