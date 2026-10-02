-- ==============================================================================
-- Migration V20: Khổ cắt theo part file (epic NGO-399 — NGO-400)
--
-- Mỗi part file có thể khai khổ cắt (vùng cắt) mà bản đã xếp được dựng theo:
--   - cut_area_length_mm: chiều dọc cuộn, trục X (100–50000, mặc định client 15000)
--   - cut_area_width_mm:  khổ phim, trục Y (100–2000, mặc định client 700)
-- Hai cột cùng NULL (chưa khai — dữ liệu cũ) hoặc cùng có giá trị trong giới hạn.
-- ==============================================================================

ALTER TABLE svg_files
    ADD COLUMN IF NOT EXISTS cut_area_length_mm INTEGER,
    ADD COLUMN IF NOT EXISTS cut_area_width_mm INTEGER;

ALTER TABLE svg_files
    ADD CONSTRAINT chk_svg_files_cut_area CHECK (
        (cut_area_length_mm IS NULL AND cut_area_width_mm IS NULL)
        OR (cut_area_length_mm BETWEEN 100 AND 50000
            AND cut_area_width_mm BETWEEN 100 AND 2000)
    );
