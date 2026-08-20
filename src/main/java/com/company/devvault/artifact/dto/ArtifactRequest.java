package com.company.devvault.artifact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ArtifactRequest {

    @NotBlank(message = "Group ID is required")
    @Size(max = 200, message = "Group ID must be at most 200 characters")
    private String groupId;

    @NotBlank(message = "Artifact ID is required")
    @Size(max = 200, message = "Artifact ID must be at most 200 characters")
    private String artifactId;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    private String packaging = "jar";

    private String language = "java";

    private String categoryName;

    @Size(max = 500, message = "Git URL must be at most 500 characters")
    private String gitUrl;

    @Size(max = 100, message = "License name must be at most 100 characters")
    private String licenseName;

    @Size(max = 50000, message = "README must be at most 50000 characters")
    private String readme;

    @Size(max = 100, message = "Developer name must be at most 100 characters")
    private String developerName;

    @Size(max = 150, message = "Developer email must be at most 150 characters")
    private String developerEmail;

    private List<String> tags = new ArrayList<>();
}