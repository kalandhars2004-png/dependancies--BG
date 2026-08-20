package com.company.devvault.artifact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VersionRequest {

    @NotBlank(message = "Version is required")
    @Size(max = 50, message = "Version must be at most 50 characters")
    private String version;

    @Size(max = 10000, message = "Release notes must be at most 10000 characters")
    private String releaseNotes;

    @Size(max = 100, message = "Developer name must be at most 100 characters")
    private String developerName;

    @Size(max = 150, message = "Developer email must be at most 150 characters")
    private String developerEmail;
}