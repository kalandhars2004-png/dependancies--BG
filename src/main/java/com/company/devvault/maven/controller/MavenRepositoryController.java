package com.company.devvault.maven.controller;

import com.company.devvault.common.exception.ApiException;
import com.company.devvault.maven.service.MavenRepositoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/repository/maven")
public class MavenRepositoryController {

    private final MavenRepositoryService mavenRepositoryService;

    public MavenRepositoryController(MavenRepositoryService mavenRepositoryService) {
        this.mavenRepositoryService = mavenRepositoryService;
    }

    @GetMapping("/**")
    public ResponseEntity<?> get(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/repository/maven";
        String path = uri.substring(prefix.length());
        if (path.isEmpty()) {
            throw ApiException.notFound("Maven repository root");
        }
        return mavenRepositoryService.resolve(path, request);
    }

    @PutMapping("/**")
    public ResponseEntity<?> put(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/repository/maven";
        String path = uri.substring(prefix.length());
        if (path.isEmpty()) {
            throw ApiException.notFound("Maven repository root");
        }
        return mavenRepositoryService.deploy(path, request);
    }
}