-- Migration V6: Add vehicle metadata fields (brand, model, year) to categories table

ALTER TABLE categories
    ADD COLUMN brand VARCHAR(100),
    ADD COLUMN model VARCHAR(100),
    ADD COLUMN "year" VARCHAR(50);

CREATE INDEX idx_categories_brand ON categories(brand);
CREATE INDEX idx_categories_model ON categories(model);
CREATE INDEX idx_categories_year ON categories("year");
