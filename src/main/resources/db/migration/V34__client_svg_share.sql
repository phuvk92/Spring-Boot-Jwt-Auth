-- ==============================================================================
-- Migration V34: Client-to-Client SVG Sharing (Internal API V2)
-- Độc lập hoàn toàn với Admin Share và cơ chế phân quyền chia sẻ trực tiếp người dùng
-- ==============================================================================

CREATE TABLE IF NOT EXISTS client_svg_shares (
    id                      BIGSERIAL PRIMARY KEY,
    share_id                VARCHAR(64) NOT NULL UNIQUE,
    user_svg_file_v2_id     BIGINT NOT NULL REFERENCES user_svg_file_v2(id) ON DELETE CASCADE,
    owner_client_id         VARCHAR(100),
    owner_user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash           VARCHAR(255) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    note                    TEXT,
    expires_at              TIMESTAMP WITHOUT TIME ZONE,
    revoked_at              TIMESTAMP WITHOUT TIME ZONE,
    revoked_by              BIGINT REFERENCES users(id) ON DELETE SET NULL,
    revoked_by_username     VARCHAR(100),
    revoke_reason           TEXT,
    last_accessed_at        TIMESTAMP WITHOUT TIME ZONE,
    verify_count            INT NOT NULL DEFAULT 0,
    preview_count           INT NOT NULL DEFAULT 0,
    download_count          INT NOT NULL DEFAULT 0,
    failed_verify_attempts  INT NOT NULL DEFAULT 0,
    token_version           INT NOT NULL DEFAULT 1,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_client_svg_shares_share_id ON client_svg_shares(share_id);
CREATE INDEX IF NOT EXISTS idx_client_svg_shares_owner_user ON client_svg_shares(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_client_svg_shares_owner_client ON client_svg_shares(owner_client_id);
CREATE INDEX IF NOT EXISTS idx_client_svg_shares_file_id ON client_svg_shares(user_svg_file_v2_id);
CREATE INDEX IF NOT EXISTS idx_client_svg_shares_status ON client_svg_shares(status);
CREATE INDEX IF NOT EXISTS idx_client_svg_shares_created_at ON client_svg_shares(created_at DESC);
