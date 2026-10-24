package com.example.svgmanager.entity.v2;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity lưu vết lịch sử gọi Internal API V2 từ client máy cắt và các ứng dụng tích hợp.
 */
@Entity
@Table(name = "audit_log_v2", indexes = {
        @Index(name = "idx_audit_log_v2_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_audit_log_v2_request_id", columnList = "request_id"),
        @Index(name = "idx_audit_log_v2_client_id", columnList = "client_id"),
        @Index(name = "idx_audit_log_v2_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_log_v2_username", columnList = "username"),
        @Index(name = "idx_audit_log_v2_dealer_id", columnList = "dealer_id"),
        @Index(name = "idx_audit_log_v2_endpoint", columnList = "endpoint"),
        @Index(name = "idx_audit_log_v2_http_status", columnList = "http_status"),
        @Index(name = "idx_audit_log_v2_result", columnList = "result")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLogV2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "client_name", length = 255)
    private String clientName;

    @Column(name = "principal_type", nullable = false, length = 50)
    private String principalType = "ANONYMOUS";

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "keycloak_user_id", length = 100)
    private String keycloakUserId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "role", length = 50)
    private String role;

    @Column(name = "dealer_id")
    private Long dealerId;

    @Column(name = "dealer_name", length = 255)
    private String dealerName;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "api_version", nullable = false, length = 20)
    private String apiVersion = "V2";

    @Column(name = "http_status", nullable = false)
    private Integer httpStatus;

    @Column(name = "result", nullable = false, length = 20)
    private String result;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "execution_time_ms", nullable = false)
    private Long executionTimeMs;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_size_bytes")
    private Long requestSizeBytes;

    @Column(name = "response_size_bytes")
    private Long responseSizeBytes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLogV2() {
    }

    public AuditLogV2(Long id, String requestId, String traceId, String clientId, String clientName,
                      String principalType, Long userId, String keycloakUserId, String username, String role,
                      Long dealerId, String dealerName, String httpMethod, String endpoint, String apiVersion,
                      Integer httpStatus, String result, String errorCode, Long executionTimeMs, String clientIp,
                      String userAgent, Long requestSizeBytes, Long responseSizeBytes, LocalDateTime createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.traceId = traceId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.principalType = principalType;
        this.userId = userId;
        this.keycloakUserId = keycloakUserId;
        this.username = username;
        this.role = role;
        this.dealerId = dealerId;
        this.dealerName = dealerName;
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.apiVersion = apiVersion;
        this.httpStatus = httpStatus;
        this.result = result;
        this.errorCode = errorCode;
        this.executionTimeMs = executionTimeMs;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.requestSizeBytes = requestSizeBytes;
        this.responseSizeBytes = responseSizeBytes;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String requestId;
        private String traceId;
        private String clientId;
        private String clientName;
        private String principalType = "ANONYMOUS";
        private Long userId;
        private String keycloakUserId;
        private String username;
        private String role;
        private Long dealerId;
        private String dealerName;
        private String httpMethod;
        private String endpoint;
        private String apiVersion = "V2";
        private Integer httpStatus;
        private String result;
        private String errorCode;
        private Long executionTimeMs;
        private String clientIp;
        private String userAgent;
        private Long requestSizeBytes;
        private Long responseSizeBytes;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder principalType(String principalType) {
            this.principalType = principalType;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder keycloakUserId(String keycloakUserId) {
            this.keycloakUserId = keycloakUserId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
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

        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public Builder httpStatus(Integer httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder result(String result) {
            this.result = result;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder executionTimeMs(Long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder requestSizeBytes(Long requestSizeBytes) {
            this.requestSizeBytes = requestSizeBytes;
            return this;
        }

        public Builder responseSizeBytes(Long responseSizeBytes) {
            this.responseSizeBytes = responseSizeBytes;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuditLogV2 build() {
            return new AuditLogV2(id, requestId, traceId, clientId, clientName, principalType,
                    userId, keycloakUserId, username, role, dealerId, dealerName, httpMethod, endpoint, apiVersion,
                    httpStatus, result, errorCode, executionTimeMs, clientIp, userAgent,
                    requestSizeBytes, responseSizeBytes, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getPrincipalType() {
        return principalType;
    }

    public void setPrincipalType(String principalType) {
        this.principalType = principalType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(String keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getRequestSizeBytes() {
        return requestSizeBytes;
    }

    public void setRequestSizeBytes(Long requestSizeBytes) {
        this.requestSizeBytes = requestSizeBytes;
    }

    public Long getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public void setResponseSizeBytes(Long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
