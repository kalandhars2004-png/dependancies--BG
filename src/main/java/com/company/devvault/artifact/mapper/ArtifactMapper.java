package com.company.devvault.artifact.mapper;

import com.company.devvault.artifact.dto.ArtifactResponse;
import com.company.devvault.artifact.dto.VersionResponse;
import com.company.devvault.artifact.entity.Artifact;
import com.company.devvault.artifact.entity.ArtifactFile;
import com.company.devvault.artifact.entity.ArtifactVersion;
import com.company.devvault.user.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArtifactMapper {

    private final UserMapper userMapper;

    public ArtifactMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public ArtifactResponse toResponse(Artifact artifact, String latestVersion, String recommendedVersion,
                                       List<String> tags, List<String> maintainers, long versionCount) {
        ArtifactResponse dto = new ArtifactResponse();
        dto.setId(artifact.getId());
        dto.setSlug(artifact.getSlug() != null ? artifact.getSlug() : artifact.getGroupId() + ":" + artifact.getArtifactId());
        dto.setGroupId(artifact.getGroupId());
        dto.setArtifactId(artifact.getArtifactId());
        dto.setName(artifact.getName());
        dto.setDescription(artifact.getDescription());
        dto.setPackaging(artifact.getPackaging());
        dto.setLanguage(artifact.getLanguage());
        dto.setRepositoryType(artifact.getRepositoryType());
        dto.setReadme(artifact.getReadme());
        dto.setGitUrl(artifact.getGitUrl());
        dto.setLicenseName(artifact.getLicenseName());
        dto.setStatus(artifact.getStatus());
        dto.setSource(artifact.getSource() != null ? artifact.getSource().name() : "INTERNAL");
        dto.setCategory(artifact.getCategory() != null ? artifact.getCategory().getName() : null);
        dto.setTags(tags);
        dto.setMaintainers(maintainers);
        if (artifact.getOwner() != null) {
            dto.setOwner(userMapper.toDto(artifact.getOwner()));
        }
        dto.setLatestVersion(latestVersion);
        dto.setRecommendedVersion(recommendedVersion);
        dto.setDownloadCount(artifact.getDownloadCount());
        dto.setVersionCount(versionCount);
        dto.setCreatedAt(artifact.getCreatedAt());
        dto.setUpdatedAt(artifact.getUpdatedAt());
        return dto;
    }

    public VersionResponse toVersionResponse(ArtifactVersion version, String publishedByName, List<VersionResponse.FileInfo> files) {
        VersionResponse dto = new VersionResponse();
        dto.setId(version.getId());
        dto.setVersion(version.getVersion());
        dto.setReleaseNotes(version.getReleaseNotes());
        dto.setRecommended(version.isRecommended());
        dto.setDeprecated(version.isDeprecated());
        dto.setDownloadCount(version.getDownloadCount());
        dto.setPublishedBy(publishedByName);
        dto.setCreatedAt(version.getCreatedAt());
        dto.setFiles(files);
        return dto;
    }

    public VersionResponse.FileInfo toFileInfo(ArtifactFile file) {
        VersionResponse.FileInfo info = new VersionResponse.FileInfo();
        info.setId(file.getId());
        info.setFileName(file.getFileName());
        info.setFileType(file.getFileType());
        info.setFileSize(file.getFileSize());
        info.setChecksum(file.getChecksum());
        return info;
    }
}