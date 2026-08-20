package com.company.devvault.search.controller;

import com.company.devvault.artifact.dto.ArtifactResponse;
import com.company.devvault.search.service.SearchService;
import com.company.devvault.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ApiResponse<List<ArtifactResponse>> search(@RequestParam(defaultValue = "") String q,
                                                      @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(searchService.search(q, limit));
    }
}