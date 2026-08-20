package com.company.devvault.audit.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AuditLogDto {

    private Long id;
    private Long userId;
    private String userName;
    private String action;
    private String entityType;
    private String entityId;
    private String metadata;
    private Instant createdAt;
}