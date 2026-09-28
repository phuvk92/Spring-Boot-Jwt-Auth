-- Migration V11: F-57 — giới hạn số máy đăng ký theo tài khoản (license theo máy, chốt 16/09)

CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(128) NOT NULL,
    device_name VARCHAR(255),
    platform VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    keycloak_session_id VARCHAR(64),
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_ip VARCHAR(64),
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(255),
    CONSTRAINT uq_user_devices_user_device UNIQUE (user_id, device_id),
    CONSTRAINT chk_user_devices_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE INDEX idx_user_devices_user_status ON user_devices(user_id, status);
CREATE INDEX idx_user_devices_session ON user_devices(keycloak_session_id);

-- Số máy tối đa riêng của một tài khoản; NULL = mặc định hệ thống (app.device.max-per-user, mặc định 1)
ALTER TABLE users ADD COLUMN max_devices INT;
ALTER TABLE users ADD CONSTRAINT chk_users_max_devices CHECK (max_devices IS NULL OR max_devices BETWEEN 1 AND 50);
