package com.company.devvault.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface ArtifactStorageService {

    void storeArtifact(String relativePath, InputStream inputStream);

    Path resolvePath(String relativePath);

    void deleteArtifact(String relativePath);

    boolean exists(String relativePath);

    Path getRoot();
}