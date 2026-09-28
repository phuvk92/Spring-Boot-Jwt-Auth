-- ==============================================================================
-- Migration V12: Design File catalog (KX-30 · KX-32 · KX-35) — hợp đồng openapi v0.3.0
--
-- Đơn vị kho là FILE (chốt 01/09): một file nhiều part, một model nhiều file.
-- `svg_files` chính là file thiết kế, chỉ thiếu danh tính hợp đồng và metadata do
-- đội nội dung nhập — nên thêm cột thay vì bảng mới. Part nằm TRONG file nên tách
-- bảng con `svg_file_parts`.
-- ==============================================================================

-- 1. Danh tính hợp đồng + metadata của file
--    file_key: id chuỗi client dùng (slug kèm khoá xe, vd
--    'abarth-695-695-2024-hatchback-3-cửa--full-body') — giữ nguyên dấu tiếng Việt.
ALTER TABLE svg_files
    ADD COLUMN IF NOT EXISTS file_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS film_usage VARCHAR(100),
    ADD COLUMN IF NOT EXISTS note TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_svg_files_file_key ON svg_files(file_key);

-- 2. Part bên trong một file (KX-32). `zone` là danh mục MỞ — chỉ hiển thị, không
--    phải cấp lọc nên giữ VARCHAR tự do, không FK sang categories.
CREATE TABLE IF NOT EXISTS svg_file_parts (
    id BIGSERIAL PRIMARY KEY,
    svg_file_id BIGINT NOT NULL REFERENCES svg_files(id) ON DELETE CASCADE,
    part_key VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    zone VARCHAR(100),
    film_usage VARCHAR(100),
    note TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_svg_file_part_key UNIQUE (svg_file_id, part_key)
);

CREATE INDEX IF NOT EXISTS idx_svg_file_parts_file_id ON svg_file_parts(svg_file_id);
