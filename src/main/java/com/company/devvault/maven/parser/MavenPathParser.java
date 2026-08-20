package com.company.devvault.maven.parser;

import com.company.devvault.artifact.entity.ArtifactFileType;
import com.company.devvault.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class MavenPathParser {

    public enum Mode {
        ARTIFACT_FILE,
        ARTIFACT_METADATA,
            VERSION_METADATA
    }

    public static final class ParsedPath {
        public final Mode mode;
        public final String groupId;
        public final String artifactId;
        public final String version;
        public final String fileName;
        public final ArtifactFileType fileType;
        public final String checksumType; // "SHA1", "SHA256", "MD5" or null
        public final String[] segments;

        ParsedPath(Mode mode, String groupId, String artifactId, String version, String fileName,
                   ArtifactFileType fileType, String checksumType, String[] segments) {
            this.mode = mode;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.fileName = fileName;
            this.fileType = fileType;
            this.checksumType = checksumType;
            this.segments = segments;
        }
    }

    public ParsedPath artifactMetadataPath(String[] segments) {
        String groupId = join(segments, 0, segments.length - 2);
        return new ParsedPath(Mode.ARTIFACT_METADATA, groupId, segments[segments.length - 2],
                null, "maven-metadata.xml", null, null, segments);
    }

    public ParsedPath versionMetadataPath(String[] segments) {
        String groupId = join(segments, 0, segments.length - 3);
        return new ParsedPath(Mode.VERSION_METADATA, groupId, segments[segments.length - 3],
                segments[segments.length - 2], "maven-metadata.xml", null, null, segments);
    }

    public ParsedPath parse(String rawPath) {
        String path = rawPath;
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String[] segments = path.split("/");
        if (segments.length < 2) {
            throw ApiException.notFound("Invalid Maven repository path: " + rawPath);
        }
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals("..") || segment.contains("\\")) {
                throw ApiException.notFound("Invalid Maven repository path: " + rawPath);
            }
        }

        String fileName = segments[segments.length - 1];

        if (fileName.equals("maven-metadata.xml")) {
            return new ParsedPath(Mode.ARTIFACT_METADATA, null, null, null, fileName, null, null, segments);
        }

        String checksumType = null;
        String baseName = fileName;
        if (fileName.endsWith(".sha1")) {
            checksumType = "SHA1";
            baseName = fileName.substring(0, fileName.length() - 5);
        } else if (fileName.endsWith(".sha256")) {
            checksumType = "SHA256";
            baseName = fileName.substring(0, fileName.length() - 7);
        } else if (fileName.endsWith(".md5")) {
            checksumType = "MD5";
            baseName = fileName.substring(0, fileName.length() - 4);
        }

        if (!(baseName.endsWith(".jar") || baseName.endsWith(".pom"))) {
            throw ApiException.notFound("Unsupported Maven file: " + fileName);
        }

        if (segments.length < 3) {
            throw ApiException.notFound("Invalid artifact path: " + rawPath);
        }
        String version = segments[segments.length - 2];
        String artifactId = segments[segments.length - 3];
        String groupId = join(segments, 0, segments.length - 3);

        ArtifactFileType fileType;
        if (baseName.endsWith(".jar")) {
            if (baseName.contains("-sources.")) {
                fileType = ArtifactFileType.SOURCES;
            } else if (baseName.contains("-javadoc.")) {
                fileType = ArtifactFileType.JAVADOC;
            } else {
                fileType = ArtifactFileType.JAR;
            }
        } else if (baseName.endsWith(".pom")) {
            fileType = ArtifactFileType.POM;
        } else {
            throw ApiException.notFound("Unsupported file type: " + fileName);
        }

        if (checksumType != null) {
            String ext = baseName.endsWith(".jar") ? ".jar" : ".pom";
            String expectedBase = artifactId + "-" + version;
            if (!baseName.startsWith(expectedBase + "-") && !baseName.equals(expectedBase + ext)) {
                throw ApiException.notFound("Checksum target does not match coordinates");
            }
        }

        return new ParsedPath(Mode.ARTIFACT_FILE, groupId, artifactId, version, fileName, fileType, checksumType,
                segments);
    }

    private String join(String[] segments, int from, int toExclusive) {
        return Arrays.stream(segments, from, toExclusive).collect(Collectors.joining("."));
    }
}