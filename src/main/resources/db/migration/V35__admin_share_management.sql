-- V35__admin_share_management.sql
-- Bổ sung trường thu hồi cho user_svg_file_v2_share và user_svg_file_share
-- Tạo view thống nhất view_unified_shares phục vụ màn hình Quản lý chia sẻ file tập trung của ADMIN

ALTER TABLE user_svg_file_v2_share
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS revoked_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS revoke_reason TEXT;

ALTER TABLE user_svg_file_share
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS revoked_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS revoke_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_user_svg_file_v2_share_created ON user_svg_file_v2_share(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_svg_file_share_created ON user_svg_file_share(created_at DESC);

-- View hợp nhất toàn bộ lượt chia sẻ (Client-to-Client & Admin/Direct User Shares)
-- view_unified_shares is mapped dynamically via Hibernate @Subselect in UnifiedShare entity
