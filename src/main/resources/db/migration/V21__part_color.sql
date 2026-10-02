-- ==============================================================================
-- Migration V21: Giữ màu tô của part từ SVG (NGO-415)
--
-- Mỗi part có thể mang màu tô hiệu lực (#RRGGBB) đọc từ file SVG (fill/style/kế thừa).
-- Phục vụ app thợ bật lại màu chuẩn của file thiết kế khi xem/cắt.
-- Cột nullable: part không có màu (hoặc none/transparent) để NULL.
-- ==============================================================================

ALTER TABLE svg_file_parts
    ADD COLUMN IF NOT EXISTS color VARCHAR(7);
