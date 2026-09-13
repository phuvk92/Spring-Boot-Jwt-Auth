-- Seed default admin user (password: Password123!)
INSERT INTO users (username, email, password, role, enabled, created_at)
VALUES (
    'admin',
    'admin@example.com',
    '$2a$10$wTfk7m7YwK3Vl0m.Z95wjeZpYIkgcO8u0nB55N3lQ5u5W9n6mE7.S',
    'ADMIN',
    TRUE,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;
