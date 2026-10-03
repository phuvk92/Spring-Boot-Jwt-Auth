-- ==============================================================================
-- Migration V23: Module Bản đã lưu của User (User Saved SVG Files)
-- Lưu trữ toàn bộ file SVG do USER lưu thông qua /api/internal/user-files
-- Phục vụ quản trị xem toàn bộ trên Admin Web qua /api/admin/user-files
-- ==============================================================================

CREATE TABLE IF NOT EXISTS user_svg_files (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL DEFAULT 'image/svg+xml',
    checksum VARCHAR(100) NOT NULL,
    category_id BIGINT REFERENCES file_categories(id) ON DELETE SET NULL,
    vehicle_node_id BIGINT REFERENCES vehicle_nodes(id) ON DELETE SET NULL,
    product_group VARCHAR(100),
    product_group_name VARCHAR(255),
    brand_name VARCHAR(255),
    model_name VARCHAR(255),
    year_from INT,
    year_to INT,
    generation_code VARCHAR(100),
    film_width DOUBLE PRECISION,
    film_width_unit VARCHAR(20) DEFAULT 'MM',
    roll_length DOUBLE PRECISION,
    roll_length_unit VARCHAR(20) DEFAULT 'MM',
    axis_x DOUBLE PRECISION,
    axis_y DOUBLE PRECISION,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dealer_id BIGINT REFERENCES dealers(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_svg_files_user_id ON user_svg_files(user_id);
CREATE INDEX IF NOT EXISTS idx_user_svg_files_dealer_id ON user_svg_files(dealer_id);
CREATE INDEX IF NOT EXISTS idx_user_svg_files_category_id ON user_svg_files(category_id);
CREATE INDEX IF NOT EXISTS idx_user_svg_files_vehicle_node_id ON user_svg_files(vehicle_node_id);
CREATE INDEX IF NOT EXISTS idx_user_svg_files_status ON user_svg_files(status);
CREATE INDEX IF NOT EXISTS idx_user_svg_files_created_at ON user_svg_files(created_at DESC);

-- Seed sample data if admin exists
DO $$
DECLARE
    v_user_id BIGINT;
    v_dealer_id BIGINT;
    v_node_id BIGINT;
    v_cat_id BIGINT;
BEGIN
    SELECT id INTO v_user_id FROM users WHERE username = 'admin' LIMIT 1;
    SELECT id INTO v_dealer_id FROM dealers LIMIT 1;
    SELECT id INTO v_node_id FROM vehicle_nodes WHERE name = 'Camry 2.5Q' LIMIT 1;
    SELECT id INTO v_cat_id FROM file_categories WHERE name = 'Ngoại thất' LIMIT 1;

    IF v_user_id IS NOT NULL THEN
        INSERT INTO user_svg_files (
            file_name,
            original_file_name,
            stored_file_name,
            file_path,
            file_size,
            mime_type,
            checksum,
            category_id,
            vehicle_node_id,
            product_group,
            product_group_name,
            brand_name,
            model_name,
            year_from,
            year_to,
            generation_code,
            film_width,
            film_width_unit,
            roll_length,
            roll_length_unit,
            axis_x,
            axis_y,
            description,
            status,
            user_id,
            dealer_id,
            created_at,
            updated_at
        ) VALUES (
            'Toyota_Camry_2.5Q_DOOR.svg',
            'Toyota_Camry_2.5Q_DOOR.svg',
            'seed-camry-door.svg',
            'seed-camry-door.svg',
            1258291,
            'image/svg+xml',
            'sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
            v_cat_id,
            v_node_id,
            'PPF_EXTERIOR',
            'PPF Ngoại thất',
            'Toyota',
            'Camry 2.5Q',
            2021,
            2024,
            'XV70',
            1520.0,
            'MM',
            3500.0,
            'MM',
            3500.0,
            1520.0,
            'Mẫu cắt cửa xe Toyota Camry 2.5Q XV70',
            'ACTIVE',
            v_user_id,
            v_dealer_id,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;
END $$;
