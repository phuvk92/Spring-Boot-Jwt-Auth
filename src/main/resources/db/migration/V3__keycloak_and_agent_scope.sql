-- Migration V3: Add Keycloak User ID and Agent ID Data Scoping

-- 1. Add keycloak_user_id and agent_id to users table
ALTER TABLE users
    ADD COLUMN keycloak_user_id VARCHAR(64) UNIQUE,
    ADD COLUMN agent_id BIGINT,
    ALTER COLUMN password DROP NOT NULL;

-- 2. Add foreign key constraint for users.agent_id referencing users(id)
ALTER TABLE users
    ADD CONSTRAINT fk_users_agent
    FOREIGN KEY (agent_id)
    REFERENCES users(id)
    ON DELETE RESTRICT;

-- 3. Add agent_id to svg_files table
ALTER TABLE svg_files
    ADD COLUMN agent_id BIGINT;

-- 4. Add foreign key constraint for svg_files.agent_id referencing users(id)
ALTER TABLE svg_files
    ADD CONSTRAINT fk_svg_agent
    FOREIGN KEY (agent_id)
    REFERENCES users(id)
    ON DELETE RESTRICT;

-- 5. Create indexes for high performance querying & data isolation
CREATE INDEX idx_users_keycloak_id ON users(keycloak_user_id);
CREATE INDEX idx_users_agent_id ON users(agent_id);
CREATE INDEX idx_svg_agent_id ON svg_files(agent_id);
