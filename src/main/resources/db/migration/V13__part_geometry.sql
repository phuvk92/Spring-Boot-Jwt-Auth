-- ==============================================================================
-- Migration V13: hình học hiển thị của part (F-56 — GET /api/v1/files/{id}/geometry)
--
-- PartOutline trong openapi v0.3.0: pathData là chuỗi đường dẫn SVG trong hệ toạ độ
-- của chính part (gốc ở góc trên-trái hộp bao); mọi kích thước/vị trí LUÔN là mm
-- (DS-86) — inch chỉ là lớp hiển thị ở client. Đây là hình học HIỂN THỊ: lệnh cắt
-- không sinh từ chuỗi này (RB-07) nên không cần đơn vị máy hay scale ở đây.
-- Cột nullable: part có thể được nạp metadata trước, hình học bổ sung sau — service
-- fail-closed về giá trị an toàn khi thiếu.
-- ==============================================================================

ALTER TABLE svg_file_parts
    ADD COLUMN IF NOT EXISTS path_data TEXT,
    ADD COLUMN IF NOT EXISTS width_mm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS height_mm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS x_mm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS y_mm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS node_count INT,
    ADD COLUMN IF NOT EXISTS hole_count INT;
