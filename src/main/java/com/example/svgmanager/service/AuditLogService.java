package com.example.svgmanager.service;

import com.example.svgmanager.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuditLogService {
    void log(String actor, String actorRole, String action, String entity, Long entityId, String details);
    Page<AuditLog> getLogs(String entity, Pageable pageable);
    Page<AuditLog> getLogs(String keyword, String entity, String actor, String actorRole, String action,
                           LocalDateTime from, LocalDateTime to, Pageable pageable);
    Optional<AuditLog> getLogById(Long id);
}
