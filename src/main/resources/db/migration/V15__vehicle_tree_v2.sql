-- ==============================================================================
-- Migration V14: Data Center v2 — cây xe 4 cấp + danh mục file
-- Theo technical/SA-DanhMucXe-v2.md §2 (chốt board 29/09)
-- Gỡ mô hình cũ: categories 6 cấp, car_brands/car_models/vehicle_configurations,
-- phân quyền đại lý theo file (Q6).
-- ==============================================================================

-- 1. Cây xe 4 cấp: BRAND › SERIES › MODEL › SUBTYPE
CREATE TABLE vehicle_nodes (
    id            BIGSERIAL PRIMARY KEY,
    parent_id     BIGINT REFERENCES vehicle_nodes(id) ON DELETE CASCADE,
    level         VARCHAR(10) NOT NULL CHECK (level IN ('BRAND','SERIES','MODEL','SUBTYPE')),
    name          VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP
);

-- Không trùng tên trong cùng cha (không phân biệt hoa thường); gốc coi như cha 0
CREATE UNIQUE INDEX uq_vehicle_nodes_parent_name
    ON vehicle_nodes (COALESCE(parent_id, 0), LOWER(name));

CREATE INDEX idx_vehicle_nodes_parent_id ON vehicle_nodes(parent_id);
CREATE INDEX idx_vehicle_nodes_level ON vehicle_nodes(level);
CREATE INDEX idx_vehicle_nodes_name ON vehicle_nodes(LOWER(name));

-- 2. Danh mục file (thay enum KX-15 cứng bên client)
CREATE TABLE file_categories (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,
    display_order INT NOT NULL DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO file_categories (id, name, display_order, active) VALUES
(1, 'Ngoại thất', 1, TRUE),
(2, 'Nội thất', 2, TRUE),
(3, 'Window film', 3, TRUE),
(4, 'Đèn & kính', 4, TRUE);

SELECT setval('file_categories_id_seq', (SELECT MAX(id) FROM file_categories));

-- 3. svg_files: cột mới của v2
ALTER TABLE svg_files
    ADD COLUMN file_category_id BIGINT REFERENCES file_categories(id),
    ADD COLUMN model_year SMALLINT CHECK (model_year IS NULL OR (model_year >= 1900 AND model_year <= 2100)),
    ADD COLUMN thumbnail_path VARCHAR(1000),
    ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'SYSTEM' CHECK (source IN ('SYSTEM','DEALER'));

CREATE INDEX idx_svg_files_file_category ON svg_files(file_category_id);
CREATE INDEX idx_svg_files_model_year ON svg_files(model_year);

-- 4. N:N file ↔ node xe (Q5; xoá node = gỡ liên kết, file còn nguyên — Q4)
CREATE TABLE svg_file_vehicle_nodes (
    svg_file_id     BIGINT NOT NULL REFERENCES svg_files(id) ON DELETE CASCADE,
    vehicle_node_id BIGINT NOT NULL REFERENCES vehicle_nodes(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (svg_file_id, vehicle_node_id)
);

CREATE INDEX idx_sfvn_node ON svg_file_vehicle_nodes(vehicle_node_id);

-- 5. Gỡ mô hình cũ (prod chỉ có dữ liệu thử)
ALTER TABLE svg_files DROP CONSTRAINT IF EXISTS fk_svg_category;
ALTER TABLE svg_files DROP CONSTRAINT IF EXISTS fk_svg_vehicle_configuration;
ALTER TABLE svg_files DROP COLUMN IF EXISTS category_id;
ALTER TABLE svg_files DROP COLUMN IF EXISTS vehicle_configuration_id;

DROP TABLE IF EXISTS svg_file_dealer_permissions;
DROP TABLE IF EXISTS svg_file_vehicle_configurations;
DROP TABLE IF EXISTS vehicle_configurations;
DROP TABLE IF EXISTS car_models;
DROP TABLE IF EXISTS car_brands;
DROP TABLE IF EXISTS categories;

-- 6. Seed cây từ META0 của design (6 hãng) + nhánh Abarth › 695 của seed cũ
--    (cũ 6 cấp brand›model›variant›year›submodel → gộp year vào model, submodel thành SUBTYPE)
INSERT INTO vehicle_nodes (id, parent_id, level, name, display_order) VALUES
-- Toyota
(1,  NULL, 'BRAND', 'Toyota', 1),
(2,  1,    'SERIES', 'Camry', 1),
(3,  2,    'MODEL', 'Camry 2.5Q', 1),
(4,  3,    'SUBTYPE', 'Bản lắp ráp VN', 1),
(5,  3,    'SUBTYPE', 'Bản nhập Thái', 2),
(6,  2,    'MODEL', 'Camry 2.0G', 2),
(7,  1,    'SERIES', 'Vios', 2),
(8,  7,    'MODEL', 'Vios 1.5G', 1),
-- VinFast
(9,  NULL, 'BRAND', 'VinFast', 2),
(10, 9,    'SERIES', 'VF 8', 1),
(11, 10,   'MODEL', 'VF 8 Plus', 1),
(12, 11,   'SUBTYPE', 'Tiêu chuẩn', 1),
(13, 9,    'SERIES', 'VF 9', 2),
(14, 13,   'MODEL', 'VF 9 Plus', 1),
-- Ford
(15, NULL, 'BRAND', 'Ford', 3),
(16, 15,   'SERIES', 'Ranger', 1),
(17, 16,   'MODEL', 'Ranger Wildtrak', 1),
(18, 17,   'SUBTYPE', 'Bản thường', 1),
-- Hyundai
(19, NULL, 'BRAND', 'Hyundai', 4),
(20, 19,   'SERIES', 'Santa Fe', 1),
(21, 20,   'MODEL', 'Santa Fe 2.5 Xăng', 1),
(22, 21,   'SUBTYPE', 'Cao cấp', 1),
-- Mazda
(23, NULL, 'BRAND', 'Mazda', 5),
(24, 23,   'SERIES', 'CX-5', 1),
(25, 24,   'MODEL', 'CX-5 2.0 Premium', 1),
-- Kia
(26, NULL, 'BRAND', 'Kia', 6),
-- Abarth (nhánh quen từ seed cũ cho mock/test client)
(27, NULL, 'BRAND', 'Abarth', 7),
(28, 27,   'SERIES', '695', 1),
(29, 28,   'MODEL', '695', 1),
(30, 28,   'MODEL', '695 Biposto', 2),
(31, 28,   'MODEL', '695 Esseesse', 3),
(32, 29,   'SUBTYPE', 'Hatchback 3 cửa', 1),
(33, 29,   'SUBTYPE', 'Cabrio', 2);

SELECT setval('vehicle_nodes_id_seq', (SELECT MAX(id) FROM vehicle_nodes));
