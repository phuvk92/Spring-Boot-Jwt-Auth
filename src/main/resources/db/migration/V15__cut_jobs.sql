-- ==============================================================================
-- Migration V14: lịch sử cắt (F-38 · KX-03) — hợp đồng openapi v0.3.0, GET /api/v1/cuts
--
-- Bảng CHỈ chứa số liệu — ràng buộc F-38 cấm hình học/biên dạng trong lịch sử cắt
-- (đại lý chỉ xem số liệu, chặn ở tầng API), nên không có cột path/geometry/point nào.
--
-- Một bản ghi gắn với MỘT máy (user_devices): scope của GET /api/v1/cuts là "máy này",
-- định danh qua claim `sid` của token → user_devices.keycloak_session_id.
--
-- `film_usage` là nhãn nguyên văn client báo; `film_usage_meters` là số đo số học để
-- tổng hợp stats.filmUsed — NULLABLE vì câu A6a ("số đo cắt là đại lượng nào") còn treo
-- với khách: endpoint báo cắt chưa chốt gửi số nào, service trả filmUsed=null khi không
-- có số liệu nguồn thay vì bịa con số.
--
-- `design_id`/`design_version` nullable: F-38 là "mở lại đúng bản đã cắt" — cột mỏng để
-- task /api/v1/designs (NGO-170) nối sau, không đưa vào DTO vì hợp đồng CutJob chưa có.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS cut_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_device_id BIGINT NOT NULL REFERENCES user_devices(id) ON DELETE CASCADE,
    cut_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    part_label VARCHAR(255),
    vehicle_label VARCHAR(255),
    film_usage VARCHAR(100),
    film_usage_meters NUMERIC(10,3),
    duration VARCHAR(50),
    outcome VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    design_id VARCHAR(255),
    design_version INT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum lưu chữ HOA (JPA EnumType.STRING); hợp đồng trả chuỗi thường ở tầng DTO.
    CONSTRAINT chk_cut_jobs_outcome CHECK (outcome IN ('COMPLETED', 'RECUT', 'MISALIGNED')),
    CONSTRAINT chk_cut_jobs_film_meters CHECK (film_usage_meters IS NULL OR film_usage_meters >= 0)
);

CREATE INDEX IF NOT EXISTS idx_cut_jobs_device_at ON cut_jobs(user_device_id, cut_at DESC);
