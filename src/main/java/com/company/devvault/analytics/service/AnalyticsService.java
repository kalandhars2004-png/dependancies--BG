package com.company.devvault.analytics.service;

import com.company.devvault.analytics.repository.DownloadEventRepository;
import com.company.devvault.artifact.repository.ArtifactFileRepository;
import com.company.devvault.artifact.repository.ArtifactRepository;
import com.company.devvault.artifact.repository.ArtifactVersionRepository;
import com.company.devvault.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ArtifactRepository artifactRepository;
    private final ArtifactVersionRepository versionRepository;
    private final ArtifactFileRepository fileRepository;
    private final UserRepository userRepository;
    private final DownloadEventRepository downloadEventRepository;

    public AnalyticsService(ArtifactRepository artifactRepository, ArtifactVersionRepository versionRepository,
                            ArtifactFileRepository fileRepository, UserRepository userRepository,
                            DownloadEventRepository downloadEventRepository) {
        this.artifactRepository = artifactRepository;
        this.versionRepository = versionRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.downloadEventRepository = downloadEventRepository;
    }

    public Map<String, Object> summary() {
        Instant now = Instant.now();
        Instant startOfMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfWeek = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalArtifacts", artifactRepository.count());
        result.put("totalVersions", versionRepository.count());
        result.put("totalDownloads", artifactRepository.sumDownloadCount());
        result.put("totalUsers", userRepository.count());
        result.put("publishedThisMonth", versionRepository.countByCreatedAtAfter(startOfMonth));
        result.put("downloadsThisWeek", downloadEventRepository.countByCreatedAtAfter(startOfWeek));
        result.put("recentActivity", recentActivity(10));
        return result;
    }

    public Map<String, Object> adminAnalytics() {
        Instant now = Instant.now();
        Instant startOfMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfWeek = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant last30Days = now.minusSeconds(30L * 24 * 3600);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalArtifacts", artifactRepository.count());
        result.put("totalVersions", versionRepository.count());
        result.put("totalDownloads", artifactRepository.sumDownloadCount());
        result.put("totalUsers", userRepository.count());
        result.put("activeUsers", userRepository.countByActive(true));
        result.put("publishedThisMonth", versionRepository.countByCreatedAtAfter(startOfMonth));
        result.put("downloadsThisWeek", downloadEventRepository.countByCreatedAtAfter(startOfWeek));

        List<Object[]> artifactDownloads = downloadEventRepository.countGroupedByArtifact();
        List<Map<String, Object>> mostDownloaded = new ArrayList<>();
        for (Object[] row : artifactDownloads) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0]);
            item.put("groupId", row[1]);
            item.put("artifactId", row[2]);
            item.put("downloads", row[3]);
            mostDownloaded.add(item);
        }
        result.put("mostDownloadedArtifacts", mostDownloaded);

        List<Object[]> userDownloads = downloadEventRepository.countGroupedByUser();
        List<Map<String, Object>> mostActiveDevelopers = new ArrayList<>();
        for (Object[] row : userDownloads) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0]);
            item.put("downloads", row[1]);
            mostActiveDevelopers.add(item);
        }
        result.put("mostActiveDevelopers", mostActiveDevelopers);

        List<Object[]> byDay = downloadEventRepository.countGroupedByDay(last30Days);
        List<Map<String, Object>> downloadsByDay = new ArrayList<>();
        for (Object[] row : byDay) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", String.valueOf(row[0]));
            item.put("downloads", row[1]);
            downloadsByDay.add(item);
        }
        result.put("downloadsByDay", downloadsByDay);

        result.put("storageFiles", fileRepository.count());
        return result;
    }

    private List<Map<String, Object>> recentActivity(int limit) {
        return downloadEventRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .limit(limit)
                .map(event -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", "DOWNLOAD");
                    item.put("user", event.getUser() != null ? event.getUser().getName() : "anonymous");
                    item.put("artifactId", event.getArtifact().getArtifactId());
                    item.put("groupArtifact", event.getArtifact().getGroupId() + ":" + event.getArtifact().getArtifactId());
                    item.put("version", event.getArtifactVersion().getVersion());
                    item.put("timestamp", event.getCreatedAt());
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}