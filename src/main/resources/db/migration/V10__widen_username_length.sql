-- V10: Widen username column length to accommodate email addresses
ALTER TABLE users ALTER COLUMN username TYPE VARCHAR(255);
