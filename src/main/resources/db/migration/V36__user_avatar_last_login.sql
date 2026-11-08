-- V36__user_avatar_last_login.sql
-- Bổ sung cột cho entity User (commit 6b6a7e7: hồ sơ cá nhân PUT /api/auth/me + ghi lần đăng nhập cuối)
-- Prod chạy ddl-auto=none nên thiếu migration này mọi truy vấn bảng users đều lỗi "column does not exist"

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar TEXT,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITHOUT TIME ZONE;
