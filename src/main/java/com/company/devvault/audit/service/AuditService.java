package com.company.devvault.audit.service;

import com.company.devvault.audit.entity.AuditLog;
import com.company.devvault.audit.repository.AuditLogRepository;
import com.company.devvault.auth.entity.User;
import com.company.devvault.auth.repository.UserRepository;
import com.company.devvault.common.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void log(String action, String entityType, String entityId, String metadata) {
        AuditLog log = new AuditLog();
        String username = SecurityUtils.currentUsername();
        if (username != null) {
            userRepository.findByEmail(username).ifPresent(user -> {
                log.setUserId(user.getId());
                log.setUserName(user.getName());
            });
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMetadata(metadata);
        auditLogRepository.save(log);
    }

    @Transactional
    public void log(String action, User actor, String entityType, String entityId, String metadata) {
        AuditLog log = new AuditLog();
        if (actor != null) {
            log.setUserId(actor.getId());
            log.setUserName(actor.getName());
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMetadata(metadata);
        auditLogRepository.save(log);
    }

    public Page<AuditLog> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.findAll(pageable);
    }
}