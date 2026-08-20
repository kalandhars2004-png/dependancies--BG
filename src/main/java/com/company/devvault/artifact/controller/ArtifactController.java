package com.company.devvault.artifact.controller;

import com.company.devvault.artifact.dto.ArtifactRequest;
import com.company.devvault.artifact.dto.ArtifactResponse;
import com.company.devvault.artifact.dto.VersionRequest;
import com.company.devvault.artifact.dto.VersionResponse;
import com.company.devvault.artifact.service.ArtifactService;
import com.company.devvault.auth.entity.User;
import com.company.devvault.auth.service.AuthService;
import com.company.devvault.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/artifacts")
public class ArtifactController {

    private final ArtifactService artifactService;
    private final AuthService authService;

    public ArtifactController(ArtifactService artifactService, AuthService authService) {
        this.artifactService = artifactService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<Page<ArtifactResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "12") int size,
                                                    @RequestParam(required = false) String sort,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) Long ownerId) {
        return ApiResponse.success(artifactService.listArtifacts(page, size, sort, categoryId, ownerId));
    }

    @GetMapping("/recent")
    public ApiResponse<List<ArtifactResponse>> recent() {
        return ApiResponse.success(artifactService.recentArtifacts(8));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<ArtifactResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.success(artifactService.getArtifactBySlug(slug));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArtifactResponse> get(@PathVariable Long id) {
        return ApiResponse.success(artifactService.getArtifact(id));
    }

    @PostMapping
    public ApiResponse<ArtifactResponse> create(@Valid @RequestBody ArtifactRequest request) {
        User actor = authService.resolvePublishActor(request.getDeveloperName(), request.getDeveloperEmail());
        return ApiResponse.success("Artifact created", artifactService.createArtifact(request, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'MAINTAINER', 'ADMIN')")
    public ApiResponse<ArtifactResponse> update(@PathVariable Long id, @Valid @RequestBody ArtifactRequest request) {
        return ApiResponse.success("Artifact updated",
                artifactService.updateArtifact(id, request, authService.getCurrentUserEntity()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        artifactService.deleteArtifact(id, authService.getCurrentUserEntity());
        return ApiResponse.success("Artifact deleted", null);
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<VersionResponse>> versions(@PathVariable Long id) {
        return ApiResponse.success(artifactService.listVersions(id));
    }

    @GetMapping("/{id}/versions/{version}")
    public ApiResponse<VersionResponse> version(@PathVariable Long id, @PathVariable String version) {
        return ApiResponse.success(artifactService.getVersion(id, version));
    }

    @PostMapping("/{id}/versions")
    public ApiResponse<VersionResponse> publishVersion(@PathVariable Long id,
                                                       @RequestPart("metadata") @Valid VersionRequest metadata,
                                                       @RequestPart("jar") MultipartFile jar,
                                                       @RequestPart(value = "pom", required = false) MultipartFile pom,
                                                       @RequestPart(value = "sources", required = false) MultipartFile sources,
                                                       @RequestPart(value = "javadoc", required = false) MultipartFile javadoc) {
        User actor = authService.resolvePublishActor(metadata.getDeveloperName(), metadata.getDeveloperEmail());
        return ApiResponse.success("Version published",
                artifactService.publishVersion(id, metadata, actor, jar, pom, sources, javadoc));
    }

    @PostMapping("/{id}/versions/{version}/recommend")
    @PreAuthorize("hasAnyRole('MAINTAINER', 'ADMIN')")
    public ApiResponse<VersionResponse> recommend(@PathVariable Long id, @PathVariable String version) {
        return ApiResponse.success("Version marked as recommended",
                artifactService.markRecommended(id, version, authService.getCurrentUserEntity()));
    }

    @PostMapping("/{id}/versions/{version}/deprecate")
    @PreAuthorize("hasAnyRole('MAINTAINER', 'ADMIN')")
    public ApiResponse<VersionResponse> deprecate(@PathVariable Long id, @PathVariable String version) {
        return ApiResponse.success("Version deprecated",
                artifactService.setDeprecated(id, version, true, authService.getCurrentUserEntity()));
    }

    @PostMapping("/{id}/versions/{version}/undeprecate")
    @PreAuthorize("hasAnyRole('MAINTAINER', 'ADMIN')")
    public ApiResponse<VersionResponse> undeprecate(@PathVariable Long id, @PathVariable String version) {
        return ApiResponse.success("Version restored",
                artifactService.setDeprecated(id, version, false, authService.getCurrentUserEntity()));
    }

    @GetMapping("/{id}/versions/{version}/files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable String version,
                                             @PathVariable Long fileId, HttpServletRequest request) {
        User user = com.company.devvault.common.util.SecurityUtils.isAuthenticated()
                ? authService.getCurrentUserEntity()
                : null;
        return artifactService.downloadFile(id, version, fileId, user,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}