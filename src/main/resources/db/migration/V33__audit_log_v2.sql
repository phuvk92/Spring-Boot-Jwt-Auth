-- ==============================================================================
-- Migration V33: Audit Log cho Internal API V2
-- Ghi nhận lịch sử gọi API Internal V2 từ client máy cắt và ứng dụng bên ngoài
-- ==============================================================================

CREATE TABLE IF NOT EXISTS audit_log_v2 (
    id                  BIGSERIAL PRIMARY KEY,
    request_id          VARCHAR(100) NOT NULL,
    trace_id            VARCHAR(100),
    client_id           VARCHAR(100),
    client_name         VARCHAR(255),
    principal_type      VARCHAR(50) NOT NULL DEFAULT 'ANONYMOUS',
    user_id             BIGINT,
    keycloak_user_id    VARCHAR(100),
    username            VARCHAR(100),
    role                VARCHAR(50),
    dealer_id           BIGINT,
    dealer_name         VARCHAR(255),
    http_method         VARCHAR(10) NOT NULL,
    endpoint            VARCHAR(500) NOT NULL,
    api_version         VARCHAR(20) NOT NULL DEFAULT 'V2',
    http_status         INTEGER NOT NULL,
    result              VARCHAR(20) NOT NULL,
    error_code          VARCHAR(100),
    execution_time_ms   BIGINT NOT NULL,
    client_ip           VARCHAR(50),
    user_agent          VARCHAR(500),
    request_size_bytes  BIGINT,
    response_size_bytes BIGINT,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_log_v2_created_at ON audit_log_v2(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_v2_request_id ON audit_log_v2(request_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_v2_client_id ON audit_log_v2(client_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_v2_user_id ON audit_log_v2(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_v2_username ON audit_log_v2(username);
CREATE INDEX IF NOT EXISTS idx_audit_log_v2_dealer_id ON audit_log_v2(dealer_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_v2_endpoint ON audit_log_v2(endpoint);
CREATE INDEX IF NOT EXISTS idx_audit_log_v2_http_status ON audit_log_v2(http_status);
CREATE INDEX IF NOT EXISTS idx_audit_log_v2_result ON audit_log_v2(result);
