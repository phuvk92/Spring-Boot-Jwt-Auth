-- Migration V5: Add Business Profile and Soft Delete support for Users

ALTER TABLE users
    ADD COLUMN full_name VARCHAR(255),
    ADD COLUMN phone VARCHAR(50),
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_users_deleted ON users(deleted);
CREATE INDEX idx_users_full_name ON users(full_name);

-- Update seeded admin full_name
UPDATE users SET full_name = 'Super Admin' WHERE username = 'admin';
