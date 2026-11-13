-- ==============================================================================
-- Migration V37: Quản lý Quota lượt Cut và Sổ cái biến động (Cut Quota Ledger)
-- ==============================================================================

-- 1. Bảng quản lý số dư Quota của từng đại lý
CREATE TABLE IF NOT EXISTS dealer_cut_quotas (
    id BIGSERIAL PRIMARY KEY,
    dealer_id BIGINT NOT NULL UNIQUE REFERENCES dealers(id) ON DELETE CASCADE,
    allocated_cuts INT NOT NULL DEFAULT 1000,
    consumed_cuts INT NOT NULL DEFAULT 0,
    remaining_cuts INT NOT NULL DEFAULT 1000,
    success_cuts INT NOT NULL DEFAULT 0,
    failed_cuts INT NOT NULL DEFAULT 0,
    refunded_cuts INT NOT NULL DEFAULT 0,
    adjusted_cuts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dealer_cut_quotas_remaining CHECK (remaining_cuts >= 0),
    CONSTRAINT chk_dealer_cut_quotas_consumed CHECK (consumed_cuts >= 0)
);

CREATE INDEX IF NOT EXISTS idx_dealer_cut_quotas_dealer ON dealer_cut_quotas(dealer_id);

-- Khởi tạo Quota mặc định cho toàn bộ đại lý hiện có chưa có quota
INSERT INTO dealer_cut_quotas (dealer_id, allocated_cuts, consumed_cuts, remaining_cuts, created_at, updated_at)
SELECT d.id, 1000, 0, 1000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM dealers d
WHERE NOT EXISTS (SELECT 1 FROM dealer_cut_quotas q WHERE q.dealer_id = d.id);

-- 2. Cập nhật bảng cut_jobs để lưu vết người dùng và đại lý trực tiếp
ALTER TABLE cut_jobs
    ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS username VARCHAR(100),
    ADD COLUMN IF NOT EXISTS dealer_id BIGINT REFERENCES dealers(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS dealer_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128),
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'SUCCESS',
    ADD COLUMN IF NOT EXISTS cuts_consumed INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS balance_before INT,
    ADD COLUMN IF NOT EXISTS balance_after INT;

CREATE INDEX IF NOT EXISTS idx_cut_jobs_dealer_at ON cut_jobs(dealer_id, cut_at DESC);
CREATE INDEX IF NOT EXISTS idx_cut_jobs_user_at ON cut_jobs(user_id, cut_at DESC);
CREATE INDEX IF NOT EXISTS idx_cut_jobs_idempotency ON cut_jobs(idempotency_key);

-- 3. Bảng sổ cái ghi nhận từng biến động số lượt Cut (Cut Quota Ledger)
CREATE TABLE IF NOT EXISTS cut_quota_ledger (
    id BIGSERIAL PRIMARY KEY,
    dealer_id BIGINT NOT NULL REFERENCES dealers(id) ON DELETE CASCADE,
    dealer_name VARCHAR(255) NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    username VARCHAR(100) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    delta INT NOT NULL,
    balance_before INT NOT NULL,
    balance_after INT NOT NULL,
    cut_job_id BIGINT REFERENCES cut_jobs(id) ON DELETE SET NULL,
    idempotency_key VARCHAR(128),
    request_id VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    ip_address VARCHAR(64),
    notes TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ledger_dealer_idempotency UNIQUE (dealer_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_cut_quota_ledger_dealer ON cut_quota_ledger(dealer_id);
CREATE INDEX IF NOT EXISTS idx_cut_quota_ledger_user ON cut_quota_ledger(user_id);
CREATE INDEX IF NOT EXISTS idx_cut_quota_ledger_created ON cut_quota_ledger(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cut_quota_ledger_job ON cut_quota_ledger(cut_job_id);
CREATE INDEX IF NOT EXISTS idx_cut_quota_ledger_type ON cut_quota_ledger(transaction_type);

-- 4. Bổ sung các cột thông tin đại lý và quota vào audit_logs
ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS dealer_id BIGINT,
    ADD COLUMN IF NOT EXISTS dealer_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS cut_delta INT,
    ADD COLUMN IF NOT EXISTS balance_after INT;

CREATE INDEX IF NOT EXISTS idx_audit_logs_dealer ON audit_logs(dealer_id);
