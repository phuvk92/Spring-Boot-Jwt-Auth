-- Migration V7: Create dealers table and link with users

CREATE TABLE dealers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(100),
    address VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(100),
    contact_person VARCHAR(100),
    plan VARCHAR(50) NOT NULL DEFAULT 'Cơ bản',
    due_date VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_dealers_code ON dealers(code);
CREATE INDEX idx_dealers_name ON dealers(name);
CREATE INDEX idx_dealers_region ON dealers(region);
CREATE INDEX idx_dealers_status ON dealers(status);
CREATE INDEX idx_dealers_deleted ON dealers(deleted);

-- Add dealer_id to users table
ALTER TABLE users
    ADD COLUMN dealer_id BIGINT,
    ADD CONSTRAINT fk_users_dealer
    FOREIGN KEY (dealer_id)
    REFERENCES dealers(id)
    ON DELETE SET NULL;

CREATE INDEX idx_users_dealer_id ON users(dealer_id);

-- Seed initial 8 dealers from PCUT POC specification
INSERT INTO dealers (code, name, region, address, phone, email, contact_person, plan, due_date, status, notes)
VALUES
('DL-0104', 'Decal Ô Tô Sài Gòn', 'TP.HCM', '124 Cộng Hoà, P.12, Q.Tân Bình, TP.HCM', '0903123456', 'saigon@decaloto.vn', 'Nguyễn Văn Thắng', 'Chuỗi', '12/2026', 'ACTIVE', 'Đại lý phân phối cấp 1'),
('DL-0111', 'PPF Hà Nội Center', 'Hà Nội', '45 Lê Văn Lương, Cầu Giấy, Hà Nội', '0912345678', 'contact@ppfhanoi.vn', 'Lê Thị Phương', 'Chuyên nghiệp', '03/2027', 'ACTIVE', 'Trung tâm thi công phim cách nhiệt & PPF'),
('DL-0128', 'Auto Decal Trường Chinh', 'TP.HCM', '350 Trường Chinh, Q.Tân Bình, TP.HCM', '0987654321', 'truongchinh@autodecal.vn', 'Võ Quốc Khánh', 'Chuyên nghiệp', '22/08/2026', 'EXPIRING', 'Gần hết hạn hợp đồng dịch vụ'),
('DL-0133', 'Film Đà Nẵng Auto', 'Đà Nẵng', '88 Nguyễn Hữu Thọ, Q.Hải Châu, Đà Nẵng', '0905123987', 'filmdanang@gmail.com', 'Đỗ Thanh Tùng', 'Chuyên nghiệp', '09/2026', 'ACTIVE', 'Chi nhánh miền Trung'),
('DL-0142', 'Cần Thơ Car Care', 'Cần Thơ', '12 Đại lộ Hoà Bình, Ninh Kiều, Cần Thơ', '0939112233', 'canthocarcare@yahoo.com', 'Bùi Thị Mai', 'Cơ bản', '05/2027', 'ACTIVE', 'Workshop dán decal Tây Nam Bộ'),
('DL-0150', 'Nha Trang Auto Film', 'Khánh Hoà', '67 23 Tháng 10, Phương Sơn, Nha Trang', '0908776655', 'nhatrangfilm@gmail.com', 'Phan Thanh Hải', 'Cơ bản', '01/2027', 'ACTIVE', 'Đại lý độc quyền Khánh Hòa'),
('DL-0158', 'Bình Dương Detailing', 'Bình Dương', '230 Đại lộ Bình Dương, Thủ Dầu Một', '0972445566', 'binhduong@detailing.vn', 'Hoàng Minh Tuấn', 'Cơ bản', '11/2026', 'ACTIVE', 'Workshop chuyên dán xe sang'),
('DL-0163', 'Hải Phòng Wrap Studio', 'Hải Phòng', '15 Lạch Tray, Ngô Quyền, Hải Phòng', '0918334455', 'hpwrap@gmail.com', 'Hoàng Gia Lâm', 'Cơ bản', '—', 'LOCKED', 'Đang tạm dừng bảo dưỡng thiết bị');
