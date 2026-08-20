package com.company.devvault.audit.controller;

import com.company.devvault.audit.dto.AuditLogDto;
import com.company.devvault.audit.mapper.AuditLogMapper;
import com.company.devvault.audit.service.AuditService;
import com.company.devvault.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditService auditService;
    private final AuditLogMapper auditLogMapper;

    public AuditLogController(AuditService auditService, AuditLogMapper auditLogMapper) {
        this.auditService = auditService;
        this.auditLogMapper = auditLogMapper;
    }

    @GetMapping
    public ApiResponse<Page<AuditLogDto>> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.success(auditService.findAll(page, size).map(auditLogMapper::toDto));
    }
}