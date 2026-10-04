-- ==============================================================================
-- Migration V25: Bổ sung ngày hết hạn tài khoản (expiration_date) cho bảng users
-- ==============================================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS expiration_date DATE NULL;

CREATE INDEX IF NOT EXISTS idx_users_expiration_date
    ON users(expiration_date);
