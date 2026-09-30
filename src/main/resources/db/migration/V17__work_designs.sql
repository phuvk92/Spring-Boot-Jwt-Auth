-- ==============================================================================
-- Migration V15: bản làm việc đã lưu của thợ (F-37 · KX-02) — hợp đồng openapi
-- v0.3.0, GET /api/v1/designs + /api/v1/designs/{id}/versions.
--
-- "Bản làm việc" là bản sao RIÊNG của thợ (chốt D1: mở mẫu = tạo bản sao, DS-84b),
-- không phải mẫu trong kho — nên tách bảng khỏi svg_files và lọc theo
-- owner_user_id, không trả lẫn mẫu gốc.
--
-- `design_key` là id chuỗi trong hợp đồng ("wd-2401") — cut_jobs.design_id (V14)
-- trỏ tới cùng khoá này để "mở lại đúng bản đã cắt" (F-38) nối sau.
--
-- Hai bảng này CHỈ chứa metadata: nơi lưu hình học bản làm việc là việc của task
-- lưu/tải sau; endpoint đọc metadata không cần biết payload nằm đâu.
--
-- `is_current` trên từng phiên bản thay vì suy ra "max number": endpoint khôi
-- phục (restore) có thể đổi phiên bản hiện hành mà không xoá bản cũ — flag do
-- task lưu/khôi phục duy trì, GET chỉ đọc.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS work_designs (
    id BIGSERIAL PRIMARY KEY,
    design_key VARCHAR(255) NOT NULL,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    vehicle_label VARCHAR(255),
    category VARCHAR(100),
    source_template_id VARCHAR(255),
    source_template_name VARCHAR(255),
    source_template_changed BOOLEAN NOT NULL DEFAULT FALSE,
    part_count INT NOT NULL DEFAULT 0,
    film_usage VARCHAR(100),
    cut_area VARCHAR(100),
    has_been_cut BOOLEAN NOT NULL DEFAULT FALSE,
    last_saved_by_device_name VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_work_designs_key UNIQUE (design_key)
);

CREATE INDEX IF NOT EXISTS idx_work_designs_owner_updated ON work_designs(owner_user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS work_design_versions (
    id BIGSERIAL PRIMARY KEY,
    work_design_id BIGINT NOT NULL REFERENCES work_designs(id) ON DELETE CASCADE,
    number INT NOT NULL,
    saved_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    saved_by_device_name VARCHAR(255),
    note TEXT,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_work_design_version_number UNIQUE (work_design_id, number),
    CONSTRAINT chk_work_design_version_number CHECK (number >= 1)
);

CREATE INDEX IF NOT EXISTS idx_work_design_versions_design ON work_design_versions(work_design_id, number DESC);
