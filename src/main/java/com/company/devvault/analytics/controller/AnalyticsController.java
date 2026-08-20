package com.company.devvault.analytics.controller;

import com.company.devvault.analytics.service.AnalyticsService;
import com.company.devvault.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.success(analyticsService.summary());
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> adminAnalytics() {
        return ApiResponse.success(analyticsService.adminAnalytics());
    }
}