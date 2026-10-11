-- Migration V31: Create user_part_category_permission table
-- Phân quyền danh mục kho mẫu & part (part_library_category) cho người dùng

CREATE TABLE IF NOT EXISTS user_part_category_permission (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    part_category_id    BIGINT NOT NULL REFERENCES part_library_category(id) ON DELETE CASCADE,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    CONSTRAINT uq_user_part_category_permission UNIQUE (user_id, part_category_id)
);

CREATE INDEX IF NOT EXISTS idx_upcp_user_id ON user_part_category_permission(user_id);
CREATE INDEX IF NOT EXISTS idx_upcp_part_category_id ON user_part_category_permission(part_category_id);
