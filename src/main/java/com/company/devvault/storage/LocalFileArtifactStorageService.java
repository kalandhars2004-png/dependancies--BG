package com.company.devvault.storage;

import com.company.devvault.common.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalFileArtifactStorageService implements ArtifactStorageService {

    private final Path root;

    public LocalFileArtifactStorageService(@Value("${devvault.storage.location}") String storageLocation) {
        this.root = Paths.get(storageLocation).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory: " + root, e);
        }
    }

    @Override
    public void storeArtifact(String relativePath, InputStream inputStream) {
        Path target = safeResolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store artifact file: " + relativePath, e);
        }
    }

    @Override
    public Path resolvePath(String relativePath) {
        Path path = safeResolve(relativePath);
        if (!Files.exists(path)) {
            throw ApiException.notFound("Artifact file not found: " + relativePath);
        }
        return path;
    }

    @Override
    public void deleteArtifact(String relativePath) {
        Path target = safeResolve(relativePath);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete artifact file: " + relativePath, e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(safeResolve(relativePath));
    }

    @Override
    public Path getRoot() {
        return root;
    }

    private Path safeResolve(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw ApiException.badRequest("Invalid storage path: " + relativePath);
        }
        return resolved;
    }
}