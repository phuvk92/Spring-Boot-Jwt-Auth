-- Migration V4: Create categories table and link svg_files with category reference
-- Based on catalog contract: 06-catalog-level.json

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    value VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    level VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id)
        REFERENCES categories(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_level ON categories(level);
CREATE INDEX idx_categories_value ON categories(value);

-- Seed Catalog Data from 06-catalog-level.json

-- Level 1: Category
INSERT INTO categories (id, value, label, level, parent_id, display_order) VALUES
(1, 'Ngoại thất', 'Ngoại thất', 'category', NULL, 1),
(2, 'Nội thất', 'Nội thất', 'category', NULL, 2),
(3, 'Window film', 'Window film', 'category', NULL, 3);

-- Level 2: Brand (under 'Ngoại thất' id 1)
INSERT INTO categories (id, value, label, level, parent_id, display_order) VALUES
(4, 'Abarth', 'Abarth', 'brand', 1, 1),
(5, 'Toyota', 'Toyota', 'brand', 1, 2),
(6, 'VinFast', 'VinFast', 'brand', 1, 3);

-- Level 3: Model (under 'Abarth' id 4)
INSERT INTO categories (id, value, label, level, parent_id, display_order) VALUES
(7, '695', '695', 'model', 4, 1),
(8, '595', '595', 'model', 4, 2);

-- Level 4: Variant (under '695' model id 7)
INSERT INTO categories (id, value, label, level, parent_id, display_order) VALUES
(9, '695', '695', 'variant', 7, 1),
(10, '695 Biposto', '695 Biposto', 'variant', 7, 2),
(11, '695 Esseesse', '695 Esseesse', 'variant', 7, 3);

-- Level 5: Year (under '695' variant id 9)
INSERT INTO categories (id, value, label, level, parent_id, display_order) VALUES
(12, '2024', '2024', 'year', 9, 1),
(13, '2023', '2023', 'year', 9, 2);

-- Level 6: Submodel (under '2024' year id 12)
INSERT INTO categories (id, value, label, level, parent_id, display_order) VALUES
(14, 'Hatchback 3 cửa', 'Hatchback 3 cửa', 'submodel', 12, 1),
(15, 'Cabrio', 'Cabrio', 'submodel', 12, 2);

-- Reset sequence for categories
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));

-- Link svg_files to categories
ALTER TABLE svg_files
    ADD COLUMN category_id BIGINT;

-- Assign existing SVG files to default category (id: 14 - Hatchback 3 cửa or id: 1 - Ngoại thất)
UPDATE svg_files SET category_id = 14 WHERE category_id IS NULL;

-- Add foreign key constraint
ALTER TABLE svg_files
    ADD CONSTRAINT fk_svg_category
    FOREIGN KEY (category_id)
    REFERENCES categories(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_svg_category_id ON svg_files(category_id);
