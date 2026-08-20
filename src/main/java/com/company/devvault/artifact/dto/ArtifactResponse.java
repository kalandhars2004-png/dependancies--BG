package com.company.devvault.artifact.dto;

import com.company.devvault.artifact.entity.ArtifactStatus;
import com.company.devvault.auth.dto.UserDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ArtifactResponse {

    private Long id;
    private String slug;
    private String groupId;
    private String artifactId;
    private String name;
    private String description;
    private String packaging;
    private String language;
    private String repositoryType;
    private String readme;
    private String gitUrl;
    private String licenseName;
    private ArtifactStatus status;
    private String source;
    private String category;
    private List<String> tags;
    private List<String> maintainers;
    private UserDto owner;
    private String latestVersion;
    private String recommendedVersion;
    private long downloadCount;
    private long versionCount;
    private Instant createdAt;
    private Instant updatedAt;
}