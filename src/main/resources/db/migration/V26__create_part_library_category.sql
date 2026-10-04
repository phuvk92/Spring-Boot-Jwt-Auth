-- Migration V26: Create part_library_category table and link with svg_files
-- Phân biệt hoàn toàn với Danh mục xe (vehicle_nodes / categories)

CREATE TABLE part_library_category (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    CONSTRAINT uq_part_library_category_code UNIQUE (code)
);

CREATE INDEX idx_part_library_category_status ON part_library_category(status);
CREATE INDEX idx_part_library_category_name ON part_library_category(name);

-- Seed dữ liệu khởi tạo cho Danh mục kho mẫu & part
-- id=1: EXTERIOR (Ngoại thất), id=2: INTERIOR (Nội thất), id=3: WINDOW_FILM (Phim cách nhiệt), id=4: LIGHTS_GLASS (Đèn & kính)
INSERT INTO part_library_category (id, code, name, status, created_by) VALUES
(1, 'EXTERIOR', 'Ngoại thất', 'ACTIVE', 'SYSTEM'),
(2, 'INTERIOR', 'Nội thất', 'ACTIVE', 'SYSTEM'),
(3, 'WINDOW_FILM', 'Phim cách nhiệt', 'ACTIVE', 'SYSTEM'),
(4, 'LIGHTS_GLASS', 'Đèn & kính', 'ACTIVE', 'SYSTEM');

SELECT setval('part_library_category_id_seq', (SELECT MAX(id) FROM part_library_category));

-- Bổ sung trường part_library_category_id vào svg_files (Part File)
ALTER TABLE svg_files
    ADD COLUMN part_library_category_id BIGINT REFERENCES part_library_category(id) ON DELETE RESTRICT;

CREATE INDEX idx_svg_files_part_library_category ON svg_files(part_library_category_id);

-- Backfill liên kết từ file_category_id sang part_library_category_id
UPDATE svg_files
SET part_library_category_id = (
    CASE file_category_id
        WHEN 1 THEN 1 -- Ngoại thất -> EXTERIOR (id=1)
        WHEN 2 THEN 2 -- Nội thất -> INTERIOR (id=2)
        WHEN 3 THEN 3 -- Window film -> WINDOW_FILM (id=3)
        WHEN 4 THEN 4 -- Đèn & kính -> LIGHTS_GLASS (id=4)
        ELSE 1        -- Mặc định
    END
)
WHERE file_category_id IS NOT NULL;
