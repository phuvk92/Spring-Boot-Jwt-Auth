-- ==============================================================================
-- Migration V22: Nội dung bản làm việc đã lưu của thợ (F-36 · NGO-429)
--
-- Bổ sung cột payload (TEXT) và payload_size (INT) cho bảng work_design_versions.
-- Payload là chuỗi JSON/chuỗi mã hoá client gửi (CL-39, AES-GCM, base64) — server
-- không giải mã, không đọc, chỉ kiểm cỡ (tối đa 20 MB).
-- Dữ liệu cũ nếu có: cho phép NULL, đọc ra coi như không mở được.
-- Các bản mới ghi vào bắt buộc NOT NULL ở tầng ứng dụng.
-- ==============================================================================

ALTER TABLE work_design_versions
    ADD COLUMN IF NOT EXISTS payload TEXT,
    ADD COLUMN IF NOT EXISTS payload_size INT;
