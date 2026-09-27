package com.example.svgmanager.service.impl;

import com.example.svgmanager.entity.AuditLog;
import com.example.svgmanager.repository.AuditLogRepository;
import com.example.svgmanager.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actor, String actorRole, String action, String entity, Long entityId, String details) {
        try {
            log.info("[AUDIT] actor='{}' role='{}' action='{}' entity='{}' entityId={} details='{}'",
                    actor, actorRole, action, entity, entityId, details);

            AuditLog auditLog = AuditLog.builder()
                    .actor(actor != null ? actor : "system")
                    .actorRole(actorRole != null ? actorRole : "SYSTEM")
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to record audit log to database: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(String entity, Pageable pageable) {
        if (entity != null && !entity.isBlank()) {
            return auditLogRepository.findByEntityOrderByTimestampDesc(entity, pageable);
        }
        return auditLogRepository.findAll(pageable);
    }
}
