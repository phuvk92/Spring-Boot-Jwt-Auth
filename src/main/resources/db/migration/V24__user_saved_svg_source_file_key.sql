-- ==============================================================================
-- Migration V24: Bổ sung source_file_key cho bảng user_svg_files (NGO-447)
-- Lưu file_key của part file trong kho (bảng svg_files) mà bản này được tạo từ.
-- ==============================================================================

ALTER TABLE user_svg_files
    ADD COLUMN IF NOT EXISTS source_file_key VARCHAR(255) NULL;

CREATE INDEX IF NOT EXISTS idx_user_svg_files_source_file_key
    ON user_svg_files(source_file_key);
