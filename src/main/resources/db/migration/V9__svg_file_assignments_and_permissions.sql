-- ==============================================================================
-- Migration V9: SVG Multi-Vehicle-Configuration Assignments & Dealer Permissions
-- ==============================================================================

-- 1. Add status and indexes to svg_files if missing
ALTER TABLE svg_files ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_svg_files_status ON svg_files(status);
CREATE INDEX IF NOT EXISTS idx_svg_files_checksum ON svg_files(checksum);

-- 2. Multi-Vehicle Configuration Assignment Table (N : N)
CREATE TABLE IF NOT EXISTS svg_file_vehicle_configurations (
    id BIGSERIAL PRIMARY KEY,
    svg_file_id BIGINT NOT NULL REFERENCES svg_files(id) ON DELETE CASCADE,
    vehicle_configuration_id BIGINT NOT NULL REFERENCES vehicle_configurations(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_svg_vehicle_config UNIQUE (svg_file_id, vehicle_configuration_id)
);

CREATE INDEX IF NOT EXISTS idx_svg_vc_svg_file_id ON svg_file_vehicle_configurations(svg_file_id);
CREATE INDEX IF NOT EXISTS idx_svg_vc_config_id ON svg_file_vehicle_configurations(vehicle_configuration_id);

-- Migrate any single vehicle_configuration_id from existing svg_files
INSERT INTO svg_file_vehicle_configurations (svg_file_id, vehicle_configuration_id, created_at)
SELECT id, vehicle_configuration_id, CURRENT_TIMESTAMP
FROM svg_files
WHERE vehicle_configuration_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- 3. Dealer Permissions Table (N : N)
CREATE TABLE IF NOT EXISTS svg_file_dealer_permissions (
    id BIGSERIAL PRIMARY KEY,
    svg_file_id BIGINT NOT NULL REFERENCES svg_files(id) ON DELETE CASCADE,
    dealer_id BIGINT NOT NULL REFERENCES dealers(id) ON DELETE CASCADE,
    can_view BOOLEAN NOT NULL DEFAULT TRUE,
    can_download BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_svg_dealer_permission UNIQUE (svg_file_id, dealer_id),
    CONSTRAINT chk_svg_dealer_permission_view_download CHECK (NOT can_download OR can_view)
);

CREATE INDEX IF NOT EXISTS idx_svg_dp_svg_file_id ON svg_file_dealer_permissions(svg_file_id);
CREATE INDEX IF NOT EXISTS idx_svg_dp_dealer_id ON svg_file_dealer_permissions(dealer_id);
CREATE INDEX IF NOT EXISTS idx_svg_dp_view_download ON svg_file_dealer_permissions(dealer_id, can_view, can_download);

-- 4. Audit Log action types support
-- (audit_logs table already exists and accepts any action string)
