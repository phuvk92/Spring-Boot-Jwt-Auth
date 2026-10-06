-- ==============================================================================
-- Migration V27: Create table user_svg_file_share for SVG file sharing
-- Cho phép chia sẻ file SVG giữa các USER mà không sao chép file vật lý
-- ==============================================================================

CREATE TABLE IF NOT EXISTS user_svg_file_share (
    id                  BIGSERIAL PRIMARY KEY,
    user_svg_file_id    BIGINT NOT NULL REFERENCES user_svg_files(id) ON DELETE CASCADE,
    shared_to_user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_by_user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_user_svg_file_share UNIQUE (user_svg_file_id, shared_to_user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_svg_file_share_to_user ON user_svg_file_share(shared_to_user_id, status);
CREATE INDEX IF NOT EXISTS idx_user_svg_file_share_file ON user_svg_file_share(user_svg_file_id, status);
