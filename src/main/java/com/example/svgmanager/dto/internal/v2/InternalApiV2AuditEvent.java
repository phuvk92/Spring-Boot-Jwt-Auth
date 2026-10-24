package com.example.svgmanager.dto.internal.v2;

public record InternalApiV2AuditEvent(
        String requestId,
        String traceId,
        String clientId,
        String clientName,
        String principalType,
        Long userId,
        String keycloakUserId,
        String username,
        String role,
        Long dealerId,
        String dealerName,
        String httpMethod,
        String endpoint,
        String apiVersion,
        Integer httpStatus,
        String result,
        String errorCode,
        Long executionTimeMs,
        String clientIp,
        String userAgent,
        Long requestSizeBytes,
        Long responseSizeBytes
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
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

        public InternalApiV2AuditEvent build() {
            return new InternalApiV2AuditEvent(
                    requestId, traceId, clientId, clientName, principalType,
                    userId, keycloakUserId, username, role, dealerId, dealerName,
                    httpMethod, endpoint, apiVersion, httpStatus, result,
                    errorCode, executionTimeMs, clientIp, userAgent,
                    requestSizeBytes, responseSizeBytes
            );
        }
    }
}
