package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_entity", columnList = "entity, entity_id"),
        @Index(name = "idx_audit_logs_timestamp", columnList = "\"timestamp\"")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(name = "actor_role", nullable = false, length = 50)
    private String actorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 100)
    private String entity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "dealer_id")
    private Long dealerId;

    @Column(name = "dealer_name", length = 255)
    private String dealerName;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "cut_delta")
    private Integer cutDelta;

    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreatedDate
    @Column(name = "\"timestamp\"", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public AuditLog() {
    }

    public AuditLog(Long id, String actor, String actorRole, String action, String entity, Long entityId,
                    Long dealerId, String dealerName, String requestId, String status, Integer cutDelta, Integer balanceAfter,
                    String details, LocalDateTime timestamp) {
        this.id = id;
        this.actor = actor;
        this.actorRole = actorRole;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.dealerId = dealerId;
        this.dealerName = dealerName;
        this.requestId = requestId;
        this.status = status;
        this.cutDelta = cutDelta;
        this.balanceAfter = balanceAfter;
        this.details = details;
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String actor;
        private String actorRole;
        private String action;
        private String entity;
        private Long entityId;
        private Long dealerId;
        private String dealerName;
        private String requestId;
        private String status;
        private Integer cutDelta;
        private Integer balanceAfter;
        private String details;
        private LocalDateTime timestamp;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder actorRole(String actorRole) {
            this.actorRole = actorRole;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder entity(String entity) {
            this.entity = entity;
            return this;
        }

        public Builder entityId(Long entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder dealerId(Long dealerId) {
            this.dealerId = dealerId;
            return this;
        }

        public Builder dealerName(String dealerName) {
            this.dealerName = dealerName;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder cutDelta(Integer cutDelta) {
            this.cutDelta = cutDelta;
            return this;
        }

        public Builder balanceAfter(Integer balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AuditLog build() {
            return new AuditLog(id, actor, actorRole, action, entity, entityId,
                    dealerId, dealerName, requestId, status, cutDelta, balanceAfter,
                    details, timestamp);
        }
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

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getDealerId() {
        return dealerId;
    }

    public void setDealerId(Long dealerId) {
        this.dealerId = dealerId;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCutDelta() {
        return cutDelta;
    }

    public void setCutDelta(Integer cutDelta) {
        this.cutDelta = cutDelta;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
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
