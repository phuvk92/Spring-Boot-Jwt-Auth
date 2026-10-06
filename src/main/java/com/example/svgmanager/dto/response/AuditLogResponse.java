package com.example.svgmanager.dto.response;

import com.example.svgmanager.entity.AuditLog;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Thông tin chi tiết bản ghi Audit Log / Activity Log")
public class AuditLogResponse {

    @Schema(description = "ID bản ghi", example = "101")
    private Long id;

    @Schema(description = "Tài khoản thực hiện", example = "operator1@gmail.com")
    private String actor;

    @Schema(description = "Vai trò người thực hiện", example = "USER")
    private String actorRole;

    @Schema(description = "Hành động thực hiện", example = "API_CALL: GET /api/internal/user-files")
    private String action;

    @Schema(description = "Tài nguyên / Entity bị tác động", example = "InternalUserFiles")
    private String entity;

    @Schema(description = "Tên hiển thị tài nguyên (alias cho Web UI)", example = "InternalUserFiles")
    private String resource;

    @Schema(description = "ID thực thể (nếu có)", example = "5001", nullable = true)
    private Long entityId;

    @Schema(description = "Chi tiết thông số, IP, thiết bị, thời gian", example = "HTTP 200 (15ms) | IP: 192.168.1.10 | Device: Roland-GX24")
    private String details;

    @Schema(description = "Thời gian ghi nhận")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public AuditLogResponse() {
    }

    public static AuditLogResponse fromEntity(AuditLog log) {
        if (log == null) return null;
        AuditLogResponse res = new AuditLogResponse();
        res.setId(log.getId());
        res.setActor(log.getActor());
        res.setActorRole(log.getActorRole());
        res.setAction(log.getAction());
        res.setEntity(log.getEntity());
        res.setResource(log.getEntity() != null
                ? (log.getEntityId() != null ? log.getEntity() + " #" + log.getEntityId() : log.getEntity())
                : "General");
        res.setEntityId(log.getEntityId());
        res.setDetails(log.getDetails());
        res.setTimestamp(log.getTimestamp());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
