-- Migration V8: Vehicle Configurations, Car Brands, Car Models, and Audit Logs

-- 1. Car Brands table
CREATE TABLE car_brands (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    display_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_car_brands_code ON car_brands(code);
CREATE INDEX idx_car_brands_status ON car_brands(status);
CREATE INDEX idx_car_brands_name ON car_brands(name);

-- 2. Car Models table
CREATE TABLE car_models (
    id BIGSERIAL PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    display_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_car_models_brand
        FOREIGN KEY (brand_id)
        REFERENCES car_brands(id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_car_models_brand_code
        UNIQUE (brand_id, code)
);

CREATE INDEX idx_car_models_brand_id ON car_models(brand_id);
CREATE INDEX idx_car_models_status ON car_models(status);
CREATE INDEX idx_car_models_code ON car_models(code);

-- 3. Vehicle Configurations table
CREATE TABLE vehicle_configurations (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    product_group VARCHAR(50) NOT NULL,
    brand_id BIGINT NOT NULL,
    model_id BIGINT NOT NULL,
    year_from INT NOT NULL,
    year_to INT NOT NULL,
    generation_code VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_vc_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_vc_brand
        FOREIGN KEY (brand_id)
        REFERENCES car_brands(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_vc_model
        FOREIGN KEY (model_id)
        REFERENCES car_models(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_vc_years
        CHECK (year_from <= year_to AND year_from >= 1900 AND year_to <= 2100)
);

CREATE INDEX idx_vc_category_id ON vehicle_configurations(category_id);
CREATE INDEX idx_vc_product_group ON vehicle_configurations(product_group);
CREATE INDEX idx_vc_brand_id ON vehicle_configurations(brand_id);
CREATE INDEX idx_vc_model_id ON vehicle_configurations(model_id);
CREATE INDEX idx_vc_year_from ON vehicle_configurations(year_from);
CREATE INDEX idx_vc_year_to ON vehicle_configurations(year_to);
CREATE INDEX idx_vc_status ON vehicle_configurations(status);
CREATE INDEX idx_vc_deleted ON vehicle_configurations(deleted);

-- Unique index ensuring no duplicate configurations for active records
CREATE UNIQUE INDEX uq_vc_business_key 
    ON vehicle_configurations (category_id, brand_id, model_id, year_from, year_to, generation_code)
    WHERE deleted = FALSE;

-- 4. Alter svg_files to optionally link directly to vehicle_configuration
ALTER TABLE svg_files
    ADD COLUMN vehicle_configuration_id BIGINT;

ALTER TABLE svg_files
    ADD CONSTRAINT fk_svg_vehicle_configuration
    FOREIGN KEY (vehicle_configuration_id)
    REFERENCES vehicle_configurations(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_svg_vehicle_configuration_id ON svg_files(vehicle_configuration_id);

-- 5. Audit Logs table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity, entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

-- 6. Seed initial Car Brands
INSERT INTO car_brands (id, code, name, status, display_order) VALUES
(1, 'TOYOTA', 'Toyota', 'ACTIVE', 1),
(2, 'VINFAST', 'VinFast', 'ACTIVE', 2),
(3, 'HONDA', 'Honda', 'ACTIVE', 3),
(4, 'HYUNDAI', 'Hyundai', 'ACTIVE', 4),
(5, 'KIA', 'Kia', 'ACTIVE', 5),
(6, 'BMW', 'BMW', 'ACTIVE', 6),
(7, 'MERCEDES_BENZ', 'Mercedes-Benz', 'ACTIVE', 7),
(8, 'FORD', 'Ford', 'ACTIVE', 8),
(9, 'MAZDA', 'Mazda', 'ACTIVE', 9),
(10, 'PORSCHE', 'Porsche', 'ACTIVE', 10);

-- Reset sequence for car_brands
SELECT setval('car_brands_id_seq', (SELECT MAX(id) FROM car_brands));

-- 7. Seed initial Car Models
INSERT INTO car_models (id, brand_id, code, name, status, display_order) VALUES
-- Toyota models (brand_id: 1)
(1, 1, 'CAMRY', 'Camry', 'ACTIVE', 1),
(2, 1, 'COROLLA_CROSS', 'Corolla Cross', 'ACTIVE', 2),
(3, 1, 'VIOS', 'Vios', 'ACTIVE', 3),
(4, 1, 'FORTUNER', 'Fortuner', 'ACTIVE', 4),
(5, 1, 'LAND_CRUISER', 'Land Cruiser', 'ACTIVE', 5),

-- VinFast models (brand_id: 2)
(6, 2, 'VF3', 'VF 3', 'ACTIVE', 1),
(7, 2, 'VF5', 'VF 5', 'ACTIVE', 2),
(8, 2, 'VF7', 'VF 7', 'ACTIVE', 3),
(9, 2, 'VF8', 'VF 8', 'ACTIVE', 4),
(10, 2, 'VF9', 'VF 9', 'ACTIVE', 5),

-- Honda models (brand_id: 3)
(11, 3, 'CRV', 'CR-V', 'ACTIVE', 1),
(12, 3, 'CIVIC', 'Civic', 'ACTIVE', 2),
(13, 3, 'CITY', 'City', 'ACTIVE', 3),

-- Hyundai models (brand_id: 4)
(14, 4, 'SANTA_FE', 'Santa Fe', 'ACTIVE', 1),
(15, 4, 'TUCSON', 'Tucson', 'ACTIVE', 2),
(16, 4, 'CRETA', 'Creta', 'ACTIVE', 3),

-- Kia models (brand_id: 5)
(17, 5, 'CARNIVAL', 'Carnival', 'ACTIVE', 1),
(18, 5, 'SELTOS', 'Seltos', 'ACTIVE', 2),
(19, 5, 'SONET', 'Sonet', 'ACTIVE', 3),

-- BMW models (brand_id: 6)
(20, 6, 'SERIES_3', '3 Series', 'ACTIVE', 1),
(21, 6, 'SERIES_5', '5 Series', 'ACTIVE', 2),
(22, 6, 'X5', 'X5', 'ACTIVE', 3),

-- Mercedes-Benz models (brand_id: 7)
(23, 7, 'C_CLASS', 'C-Class', 'ACTIVE', 1),
(24, 7, 'E_CLASS', 'E-Class', 'ACTIVE', 2),
(25, 7, 'GLC', 'GLC', 'ACTIVE', 3),

-- Ford models (brand_id: 8)
(26, 8, 'RANGER', 'Ranger', 'ACTIVE', 1),
(27, 8, 'EVEREST', 'Everest', 'ACTIVE', 2),

-- Mazda models (brand_id: 9)
(28, 9, 'CX5', 'CX-5', 'ACTIVE', 1),
(29, 9, 'MAZDA3', 'Mazda 3', 'ACTIVE', 2),

-- Porsche models (brand_id: 10)
(30, 10, 'MACAN', 'Macan', 'ACTIVE', 1),
(31, 10, 'CAYENNE', 'Cayenne', 'ACTIVE', 2),
(32, 10, 'PANAMERA', 'Panamera', 'ACTIVE', 3);

-- Reset sequence for car_models
SELECT setval('car_models_id_seq', (SELECT MAX(id) FROM car_models));

-- 8. Seed initial Vehicle Configurations for 3 Product Groups
-- Category IDs: 1 (Ngoại thất / PPF Exterior), 2 (Nội thất / PPF Interior), 3 (Window film / Window Film)
INSERT INTO vehicle_configurations (id, category_id, product_group, brand_id, model_id, year_from, year_to, generation_code, status, deleted) VALUES
-- PPF Exterior
(1, 1, 'PPF_EXTERIOR', 1, 1, 2019, 2023, 'XV70', 'ACTIVE', FALSE),
(2, 1, 'PPF_EXTERIOR', 1, 1, 2024, 2026, 'XV80', 'ACTIVE', FALSE),
(3, 1, 'PPF_EXTERIOR', 2, 9, 2022, 2025, 'VF8-GEN1', 'ACTIVE', FALSE),
(4, 1, 'PPF_EXTERIOR', 2, 6, 2024, 2026, 'VF3-BASE', 'ACTIVE', FALSE),
(5, 1, 'PPF_EXTERIOR', 6, 22, 2019, 2024, 'G05', 'ACTIVE', FALSE),
(6, 1, 'PPF_EXTERIOR', 7, 25, 2023, 2026, 'X254', 'ACTIVE', FALSE),

-- PPF Interior
(7, 2, 'PPF_INTERIOR', 1, 1, 2019, 2023, 'XV70', 'ACTIVE', FALSE),
(8, 2, 'PPF_INTERIOR', 2, 9, 2022, 2025, 'VF8-GEN1', 'ACTIVE', FALSE),
(9, 2, 'PPF_INTERIOR', 6, 22, 2019, 2024, 'G05', 'ACTIVE', FALSE),

-- Window Film
(10, 3, 'WINDOW_FILM', 1, 1, 2019, 2023, 'XV70', 'ACTIVE', FALSE),
(11, 3, 'WINDOW_FILM', 2, 9, 2022, 2025, 'VF8-GEN1', 'ACTIVE', FALSE),
(12, 3, 'WINDOW_FILM', 7, 25, 2023, 2026, 'X254', 'ACTIVE', FALSE);

-- Reset sequence for vehicle_configurations
SELECT setval('vehicle_configurations_id_seq', (SELECT MAX(id) FROM vehicle_configurations));
