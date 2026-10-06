package com.example.svgmanager.service.impl;

import com.example.svgmanager.entity.AuditLog;
import com.example.svgmanager.repository.AuditLogRepository;
import com.example.svgmanager.repository.AuditLogSpecification;
import com.example.svgmanager.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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

            String safeActor = actor != null ? (actor.length() > 100 ? actor.substring(0, 100) : actor) : "system";
            String safeRole = actorRole != null ? (actorRole.length() > 50 ? actorRole.substring(0, 50) : actorRole) : "SYSTEM";
            String safeAction = action != null ? (action.length() > 100 ? action.substring(0, 100) : action) : "UNKNOWN";
            String safeEntity = entity != null ? (entity.length() > 100 ? entity.substring(0, 100) : entity) : "GENERAL";

            AuditLog auditLog = AuditLog.builder()
                    .actor(safeActor)
                    .actorRole(safeRole)
                    .action(safeAction)
                    .entity(safeEntity)
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
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(String keyword, String entity, String actor, String actorRole, String action,
                                   LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<AuditLog> spec = AuditLogSpecification.filter(keyword, entity, actor, actorRole, action, from, to);
        return auditLogRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditLog> getLogById(Long id) {
        return auditLogRepository.findById(id);
    }
}
