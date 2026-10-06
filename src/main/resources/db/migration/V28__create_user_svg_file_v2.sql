-- ==============================================================================
-- Migration V28: Module Bản đã lưu của User V2 (Encrypted User SVG Files V2)
-- Kiến trúc V2 độc lập hoàn toàn với V1: schema riêng, storage riêng, AES-256-GCM
-- ==============================================================================

CREATE TABLE IF NOT EXISTS user_svg_file_v2 (
    id                      BIGSERIAL PRIMARY KEY,
    file_name               VARCHAR(255) NOT NULL,
    original_file_name      VARCHAR(255) NOT NULL,
    storage_key             VARCHAR(1000) NOT NULL,
    file_path               VARCHAR(1000) NOT NULL,
    file_size               BIGINT NOT NULL,
    mime_type               VARCHAR(100) NOT NULL DEFAULT 'image/svg+xml',
    plaintext_checksum      VARCHAR(100) NOT NULL,
    encrypted_checksum      VARCHAR(100) NOT NULL,

    -- Encryption metadata (AES-256-GCM envelope encryption)
    encryption_algorithm    VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    encryption_key_version  VARCHAR(50) NOT NULL DEFAULT 'v1',
    encrypted_dek           BYTEA NOT NULL,
    encryption_iv           BYTEA NOT NULL,

    -- Business metadata
    category_id             BIGINT REFERENCES file_categories(id) ON DELETE SET NULL,
    vehicle_node_id         BIGINT REFERENCES vehicle_nodes(id) ON DELETE SET NULL,
    product_group           VARCHAR(100),
    product_group_name      VARCHAR(255),
    brand_name              VARCHAR(255),
    model_name              VARCHAR(255),
    year_from               INT,
    year_to                 INT,
    generation_code         VARCHAR(100),
    film_width              DOUBLE PRECISION,
    film_width_unit         VARCHAR(20) DEFAULT 'MM',
    roll_length             DOUBLE PRECISION,
    roll_length_unit        VARCHAR(20) DEFAULT 'MM',
    axis_x                  DOUBLE PRECISION,
    axis_y                  DOUBLE PRECISION,
    source_file_key         VARCHAR(255),
    description             TEXT,

    -- Ownership & status
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dealer_id               BIGINT REFERENCES dealers(id) ON DELETE SET NULL,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_svg_file_v2_user_id ON user_svg_file_v2(user_id);
CREATE INDEX IF NOT EXISTS idx_user_svg_file_v2_dealer_id ON user_svg_file_v2(dealer_id);
CREATE INDEX IF NOT EXISTS idx_user_svg_file_v2_category_id ON user_svg_file_v2(category_id);
CREATE INDEX IF NOT EXISTS idx_user_svg_file_v2_vehicle_node_id ON user_svg_file_v2(vehicle_node_id);
CREATE INDEX IF NOT EXISTS idx_user_svg_file_v2_status ON user_svg_file_v2(status);
CREATE INDEX IF NOT EXISTS idx_user_svg_file_v2_created_at ON user_svg_file_v2(created_at DESC);

-- Table chia sẻ file V2 riêng biệt hoàn toàn với V1
CREATE TABLE IF NOT EXISTS user_svg_file_v2_share (
    id                      BIGSERIAL PRIMARY KEY,
    user_svg_file_v2_id     BIGINT NOT NULL REFERENCES user_svg_file_v2(id) ON DELETE CASCADE,
    shared_to_user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_by_user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_user_svg_file_v2_share UNIQUE (user_svg_file_v2_id, shared_to_user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_svg_file_v2_share_to_user ON user_svg_file_v2_share(shared_to_user_id, status);
CREATE INDEX IF NOT EXISTS idx_user_svg_file_v2_share_file ON user_svg_file_v2_share(user_svg_file_v2_id, status);
