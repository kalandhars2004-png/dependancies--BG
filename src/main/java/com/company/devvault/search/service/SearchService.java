package com.company.devvault.search.service;

import com.company.devvault.artifact.dto.ArtifactResponse;
import com.company.devvault.artifact.entity.Artifact;
import com.company.devvault.artifact.entity.ArtifactVersion;
import com.company.devvault.artifact.mapper.ArtifactMapper;
import com.company.devvault.artifact.repository.ArtifactRepository;
import com.company.devvault.artifact.repository.ArtifactTagRepository;
import com.company.devvault.artifact.repository.ArtifactVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final ArtifactRepository artifactRepository;
    private final ArtifactVersionRepository versionRepository;
    private final ArtifactTagRepository artifactTagRepository;
    private final ArtifactMapper artifactMapper;

    public SearchService(ArtifactRepository artifactRepository, ArtifactVersionRepository versionRepository,
                         ArtifactTagRepository artifactTagRepository, ArtifactMapper artifactMapper) {
        this.artifactRepository = artifactRepository;
        this.versionRepository = versionRepository;
        this.artifactTagRepository = artifactTagRepository;
        this.artifactMapper = artifactMapper;
    }

    public List<ArtifactResponse> search(String query, int limit) {
        String q = query == null ? "" : query.trim();
        List<Artifact> artifacts;
        if (q.isEmpty()) {
            artifacts = artifactRepository.findTop10ByOrderByCreatedAtDesc();
        } else {
            artifacts = artifactRepository.search(q);
        }
        return artifacts.stream().limit(Math.min(limit, 100)).map(this::toResponse).collect(Collectors.toList());
    }

    private ArtifactResponse toResponse(Artifact artifact) {
        List<String> tags = artifactTagRepository.findByArtifactId(artifact.getId()).stream()
                .map(at -> at.getTag().getName())
                .collect(Collectors.toList());
        String latest = versionRepository.findFirstByArtifactIdOrderByCreatedAtDesc(artifact.getId())
                .map(ArtifactVersion::getVersion).orElse(null);
        String recommended = versionRepository.findFirstByArtifactIdAndRecommendedTrueOrderByCreatedAtDesc(artifact.getId())
                .map(ArtifactVersion::getVersion).orElse(null);
        long versionCount = versionRepository.countByArtifactId(artifact.getId());
        return artifactMapper.toResponse(artifact, latest, recommended, tags, List.of(), versionCount);
    }
}