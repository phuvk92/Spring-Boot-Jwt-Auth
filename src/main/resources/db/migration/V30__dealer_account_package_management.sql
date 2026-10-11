-- ==============================================================================
-- Migration V30: Quản lý gói account đại lý theo tổng quỹ ngày sử dụng
-- ==============================================================================

-- 1. Bổ sung các cột thông tin gói account cho bảng dealers
ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS max_accounts INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS total_allocated_days INTEGER NOT NULL DEFAULT 3600,
    ADD COLUMN IF NOT EXISTS used_allocated_days INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS package_start_date DATE DEFAULT CURRENT_DATE,
    ADD COLUMN IF NOT EXISTS package_expiry_date DATE,
    ADD COLUMN IF NOT EXISTS package_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- Cập nhật ngày hết hạn mặc định cho các đại lý hiện có (1 năm = 360 ngày theo quy ước nghiệp vụ)
UPDATE dealers
SET package_expiry_date = CURRENT_DATE + 360
WHERE package_expiry_date IS NULL;

CREATE INDEX IF NOT EXISTS idx_dealers_pkg_status ON dealers(package_status);
CREATE INDEX IF NOT EXISTS idx_dealers_pkg_expiry ON dealers(package_expiry_date);

-- 2. Bổ sung trường số ngày được cấp từ quỹ cho từng user trong bảng users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS allocated_days INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_users_allocated_days ON users(allocated_days);

-- 3. Tạo bảng lưu vết lịch sử cấp gói và biến động quỹ ngày của đại lý
CREATE TABLE IF NOT EXISTS dealer_package_history (
    id BIGSERIAL PRIMARY KEY,
    dealer_id BIGINT NOT NULL REFERENCES dealers(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    previous_max_accounts INTEGER,
    new_max_accounts INTEGER,
    previous_total_days INTEGER,
    new_total_days INTEGER,
    used_days INTEGER,
    change_days INTEGER,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dealer_pkg_hist_dealer ON dealer_package_history(dealer_id);
CREATE INDEX IF NOT EXISTS idx_dealer_pkg_hist_user ON dealer_package_history(user_id);
CREATE INDEX IF NOT EXISTS idx_dealer_pkg_hist_created ON dealer_package_history(created_at);
