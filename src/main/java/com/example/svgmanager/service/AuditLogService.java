package com.example.svgmanager.service;

import com.example.svgmanager.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void log(String actor, String actorRole, String action, String entity, Long entityId, String details);
    Page<AuditLog> getLogs(String entity, Pageable pageable);
}
