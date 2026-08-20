package com.company.devvault.analytics.repository;

import com.company.devvault.analytics.entity.DownloadEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DownloadEventRepository extends JpaRepository<DownloadEvent, Long> {

    long countByCreatedAtAfter(Instant after);

    void deleteByArtifactId(Long artifactId);

    long count();

    List<DownloadEvent> findTop20ByOrderByCreatedAtDesc();

    @Query("SELECT d.artifact.name, d.artifact.groupId, d.artifact.artifactId, COUNT(d) " +
            "FROM DownloadEvent d GROUP BY d.artifact.id, d.artifact.name, d.artifact.groupId, d.artifact.artifactId " +
            "ORDER BY COUNT(d) DESC")
    List<Object[]> countGroupedByArtifact();

    @Query("SELECT d.user.name, COUNT(d) FROM DownloadEvent d WHERE d.user IS NOT NULL " +
            "GROUP BY d.user.name ORDER BY COUNT(d) DESC")
    List<Object[]> countGroupedByUser();

    @Query("SELECT FUNCTION('DATE', d.createdAt), COUNT(d) FROM DownloadEvent d " +
            "WHERE d.createdAt >= :from GROUP BY FUNCTION('DATE', d.createdAt) ORDER BY FUNCTION('DATE', d.createdAt)")
    List<Object[]> countGroupedByDay(@Param("from") Instant from);
}