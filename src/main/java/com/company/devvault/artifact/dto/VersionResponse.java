package com.company.devvault.artifact.dto;

import com.company.devvault.artifact.entity.ArtifactFileType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VersionResponse {

    private Long id;
    private String version;
    private String releaseNotes;
    private boolean recommended;
    private boolean deprecated;
    private long downloadCount;
    private String publishedBy;
    private Instant createdAt;
    private List<FileInfo> files;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FileInfo {
        private Long id;
        private String fileName;
        private ArtifactFileType fileType;
        private long fileSize;
        private String checksum;
    }
}