package com.company.devvault.common.util;

import com.company.devvault.common.exception.ApiException;

import java.util.regex.Pattern;

public final class MavenCoordinates {

    private static final Pattern GROUP_ID = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");
    private static final Pattern ARTIFACT_ID = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]*$");
    private static final Pattern VERSION = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");

    private MavenCoordinates() {
    }

    public static void validate(String groupId, String artifactId, String version) {
        if (groupId == null || !GROUP_ID.matcher(groupId).matches()) {
            throw ApiException.badRequest("Invalid groupId: " + groupId + " (must match " + GROUP_ID.pattern() + ")");
        }
        if (artifactId == null || !ARTIFACT_ID.matcher(artifactId).matches()) {
            throw ApiException.badRequest("Invalid artifactId: " + artifactId + " (must match " + ARTIFACT_ID.pattern() + ")");
        }
        if (version == null || !VERSION.matcher(version).matches()) {
            throw ApiException.badRequest("Invalid version: " + version + " (must match " + VERSION.pattern() + ")");
        }
    }

    public static void validateVersion(String version) {
        if (version == null || !VERSION.matcher(version).matches()) {
            throw ApiException.badRequest("Invalid version: " + version);
        }
    }

    public static String toPath(String groupId, String artifactId) {
        return groupId.replace('.', '/') + "/" + artifactId;
    }
}