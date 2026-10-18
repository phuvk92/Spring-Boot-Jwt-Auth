-- ==============================================================================
-- Migration V32: Liên kết đại lý và tài khoản AGENT duy nhất
-- ==============================================================================

-- 1. Bổ sung cột agent_user_id vào bảng dealers
ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS agent_user_id BIGINT;

-- 2. Thêm khóa ngoại tham chiếu users(id) ON DELETE SET NULL nếu chưa có
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_dealers_agent_user'
    ) THEN
        ALTER TABLE dealers
            ADD CONSTRAINT fk_dealers_agent_user
            FOREIGN KEY (agent_user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 3. Tạo partial unique index trên users để đảm bảo mỗi đại lý chỉ có duy nhất 1 tài khoản AGENT
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_dealer_agent
    ON users (dealer_id)
    WHERE role = 'AGENT' AND deleted = false;

-- 4. Unique index trên dealers.agent_user_id để một user không thể là AGENT của nhiều đại lý
CREATE UNIQUE INDEX IF NOT EXISTS uq_dealers_agent_user_id
    ON dealers (agent_user_id)
    WHERE agent_user_id IS NOT NULL;

-- 5. Đồng bộ reconcile dữ liệu cũ: nếu đại lý đã có user role AGENT thì liên kết vào agent_user_id
UPDATE dealers d
SET agent_user_id = u.id
FROM users u
WHERE u.dealer_id = d.id
  AND u.role = 'AGENT'
  AND u.deleted = false
  AND d.agent_user_id IS NULL;
