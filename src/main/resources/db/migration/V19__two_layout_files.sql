-- ==============================================================================
-- Migration V19: Part file hai cách xếp (SA-DanhMucXe-v2 §8 — NGO-378)
--
-- Mỗi part file có tối đa 2 SVG: đã xếp (NESTED) và chưa xếp (RAW).
-- 1. svg_file_parts thêm cột layout ('NESTED' / 'RAW'), mặc định 'NESTED' cho dữ liệu cũ;
--    đổi unique constraint sang (svg_file_id, layout, part_key).
-- 2. svg_files thêm các cột bản raw (raw_stored_filename, raw_original_filename,
--    raw_file_path, raw_file_size, raw_checksum) và cho phép các cột bản nested
--    nullable khi file chỉ có bản raw.
-- ==============================================================================

-- 1. Thêm cột layout cho svg_file_parts
ALTER TABLE svg_file_parts
    ADD COLUMN IF NOT EXISTS layout VARCHAR(8) NOT NULL DEFAULT 'NESTED'
    CONSTRAINT chk_svg_file_parts_layout CHECK (layout IN ('NESTED', 'RAW'));

-- 2. Đổi unique constraint từ (svg_file_id, part_key) sang (svg_file_id, layout, part_key)
ALTER TABLE svg_file_parts
    DROP CONSTRAINT IF EXISTS uq_svg_file_part_key;

ALTER TABLE svg_file_parts
    ADD CONSTRAINT uq_svg_file_part_layout_key UNIQUE (svg_file_id, layout, part_key);

-- 3. Cho phép nullable trên các cột bản nested của svg_files
ALTER TABLE svg_files
    ALTER COLUMN original_filename DROP NOT NULL,
    ALTER COLUMN stored_filename DROP NOT NULL,
    ALTER COLUMN file_path DROP NOT NULL,
    ALTER COLUMN file_size DROP NOT NULL;

-- 4. Thêm các cột cho bản raw trên svg_files
ALTER TABLE svg_files
    ADD COLUMN IF NOT EXISTS raw_stored_filename VARCHAR(255),
    ADD COLUMN IF NOT EXISTS raw_original_filename VARCHAR(255),
    ADD COLUMN IF NOT EXISTS raw_file_path VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS raw_file_size BIGINT,
    ADD COLUMN IF NOT EXISTS raw_checksum VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_svg_file_parts_layout ON svg_file_parts(svg_file_id, layout);
