-- Migration V15: BR-10 — thương hiệu đại lý pha 2 (GET /api/v1/branding, RB-08)
-- Một đại lý tối đa một dòng cấu hình. Thiếu dòng hoặc trường NULL = dùng thương hiệu
-- sản phẩm Pcut (BR-30 user độc lập, BR-32 đại lý chưa cấu hình) — service quyết định,
-- không seed mặc định vào đây.
CREATE TABLE dealer_branding (
    dealer_id BIGINT PRIMARY KEY REFERENCES dealers(id) ON DELETE CASCADE,
    display_name VARCHAR(255),
    slogan VARCHAR(255),
    hotline VARCHAR(50),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    theme VARCHAR(10),
    allow_worker_theme_toggle BOOLEAN,
    show_dealer_name_next_to_logo BOOLEAN,
    protect_screen_capture BOOLEAN,
    logo_png BYTEA,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    -- Bảng 5 màu cố định của thiết kế (BR-40) — không phải color picker tự do
    CONSTRAINT chk_dealer_branding_primary_color CHECK (
        primary_color IS NULL OR primary_color IN ('#2563C9', '#7C3AED', '#2E7D5B', '#C2452D', '#35342F')
    ),
    CONSTRAINT chk_dealer_branding_theme CHECK (theme IS NULL OR theme IN ('light', 'dark', 'system')),
    -- Trần kích thước asset pha 2 (BR-12): 1MB
    CONSTRAINT chk_dealer_branding_logo_size CHECK (logo_png IS NULL OR octet_length(logo_png) <= 1048576)
);
