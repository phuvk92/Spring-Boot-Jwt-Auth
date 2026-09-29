# PCUT - HỆ THỐNG QUẢN TRỊ (ADMIN SYSTEM)

Production-ready backend REST API built with **Java 21**, **Spring Boot 3.3.5**, **PostgreSQL**, **Keycloak / JWT Authentication**, and **Docker** for enterprise-grade User and SVG File Management with advanced XSS & XXE protection.

Thiết kế giao diện và kiến trúc tuân thủ theo chuẩn thiết kế **PCUT Admin POC**:
- **TỔNG QUAN**: Bảng tổng quan giám sát sản lượng, lượt cắt, thợ đang hoạt động và cảnh báo hệ thống *(đã loại bỏ hoàn toàn các mục Báo Cáo, Vận Hành và KINH DOANH / Doanh thu)*.
- **NỀN TẢNG & TÀI KHOẢN**: Quản lý Đại lý & Chi nhánh, Quản lý người dùng, Quản lý phiên & Thiết bị, Nhật ký quản trị (Audit Log).
- **DATA CENTER**: Quản lý cây xe 4 cấp (Hãng › Dòng xe › Model › Phiên bản), danh mục file, Kho mẫu & Part file (SVG), Nạp mẫu hàng loạt.

---

## 🚀 1. Giới thiệu dự án

PCUT Admin Backend cung cấp một hệ sinh thái an toàn để:
- Quản lý tài khoản người dùng và phân quyền RBAC (`ADMIN`, `AGENT`, `USER`).
- Xác thực và phân quyền bằng JWT Token & Refresh Token qua Keycloak Identity Provider.
- Tải lên, xử lý khử độc (sanitization) chống mã độc XSS / XXE cho các file SVG.
- Lưu trữ file theo cấu trúc phân cấp thời gian `/data/svg/YYYY/MM/<uuid>.svg`.
- Xem trước trực tiếp (inline preview) an toàn với header bảo mật (`X-Content-Type-Options: nosniff`).
- Tải xuống file (streaming download) và tính toán mã băm SHA-256 cho mỗi file.
- Ghi log kiểm toán (Audit Logging) chuẩn hóa truy vết bảo mật hệ thống.
- Quản lý cây xe 4 cấp `BRAND › SERIES › MODEL › SUBTYPE` (Data Center v2, migration V14) với API quản trị phân trang theo hãng, tìm kiếm giữ tổ tiên, đổi tên, xoá nhánh không chặn.
- Danh mục file (`file_categories`): Ngoại thất · Nội thất · Window film · Đèn & kính; file SVG gắn nhiều mẫu xe qua `svg_file_vehicle_nodes`.

---

## 🛠️ 2. Công nghệ sử dụng

- **Ngôn ngữ & Nền tảng:** Java 21, Spring Boot 3.3.5
- **Khung ứng dụng:**
  - Spring Web (REST API)
  - Spring Data JPA (PostgreSQL Persistence & Dynamic Specifications)
  - Spring Security (Stateless OAuth2 Resource Server & Method Security `@PreAuthorize`)
  - Spring Validation (Jakarta Bean Validation)
  - Spring Boot Actuator (Healthcheck & Metrics)
- **Cơ sở dữ liệu:** PostgreSQL 16 & Flyway Database Migration
- **Bảo mật & Parser:**
  - Keycloak 25 (Identity & Access Management)
  - Jsoup 1.18.1 (DOM SVG Sanitizer)
  - DOM XML SAX Parser (Chặn tuyệt đối XXE, DTDs & External Entities)
- **Tài liệu API:** OpenAPI 3 / SpringDoc Swagger UI
- **Kiểm thử:** JUnit 5, AssertJ, Mockito, MockMvc, H2 In-Memory DB
- **Containerization:** Docker (Multi-stage Eclipse Temurin 21) & Docker Compose

---

## 📁 3. Cấu trúc thư mục (Architecture)

```text
com.example.svgmanager
├── config                      # Cấu hình Security, CORS, Swagger OpenAPI
│   ├── OpenApiConfig.java
│   └── SecurityConfig.java
├── controller                  # REST API Endpoints
│   ├── AuthController.java     # /api/auth (Login, Refresh, Me, Change Password)
│   ├── VehicleNodeController.java # /api/vehicle-nodes (CRUD cây xe 4 cấp — ADMIN)
│   ├── SvgController.java      # /api/svg (Upload, List, Preview, Download, Delete)
│   └── UserController.java     # /api/users (Admin CRUD, Role, Status)
├── dto
│   ├── request                 # DTO đầu vào (Login, Update, Create, Change Password)
│   └── response                # DTO đầu ra (Auth, User, Svg, Category, Page, Error, Message)
├── entity                      # JPA Entities
│   ├── VehicleNode.java        # Cây xe 4 cấp BRAND›SERIES›MODEL›SUBTYPE
│   ├── FileCategory.java       # Danh mục file (Ngoại thất, Window film…)
│   ├── Role.java               # Enum: ADMIN, AGENT, USER
│   ├── SvgFile.java
│   └── User.java
├── exception                   # Custom Exceptions & GlobalExceptionHandler
├── mapper                      # DTO Entity Mappers
├── repository                  # Spring Data JPA Repositories
│   ├── VehicleNodeRepository.java
│   ├── FileCategoryRepository.java
│   ├── SvgFileRepository.java
│   └── UserRepository.java
├── security                    # Security Filters, Handlers, Converter
├── service                     # Business Logic Interfaces
│   ├── AuthService.java
│   ├── CategoryService.java
│   ├── FileStorageService.java
│   ├── SvgSanitizerService.java
│   ├── SvgService.java
│   └── UserService.java
│   └── impl                    # Service Implementations
└── util                        # Helper Utilities (SHA-256 Checksum, Path Traversal)
```

---

## 🔐 4. Phân quyền và Vai trò (RBAC)

| Chức năng | Endpoint | ADMIN | AGENT | USER | Unauthenticated |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Đăng nhập** | `POST /api/auth/login` | ✅ | ✅ | ✅ | ✅ |
| **Refresh Token** | `POST /api/auth/refresh` | ✅ | ✅ | ✅ | ✅ |
| **Thông tin cá nhân** | `GET /api/auth/me` | ✅ | ✅ | ✅ | ❌ |
| **Đổi mật khẩu** | `POST /api/auth/change-password` | ✅ | ✅ | ✅ | ❌ |
| **Danh sách User** | `GET /api/users` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Chi tiết User** | `GET /api/users/{id}` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Tạo User mới** | `POST /api/users` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Cập nhật User** | `PUT /api/users/{id}` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Đổi trạng thái User** | `PATCH /api/users/{id}/status` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Đổi vai trò User** | `PATCH /api/users/{id}/role` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Xóa User** | `DELETE /api/users/{id}` | ✅ *(Trừ bản thân & User có SVG)* | ❌ (403) | ❌ (403) | ❌ (401) |
| **Tải lên SVG** | `POST /api/svg/upload` | ✅ | ✅ | ❌ (403) | ❌ (401) |
| **Danh sách SVG** | `GET /api/svg` | ✅ | ✅ | ✅ | ❌ (401) |
| **Chi tiết SVG** | `GET /api/svg/{id}` | ✅ | ✅ | ✅ | ❌ (401) |
| **Xem trước SVG** | `GET /api/svg/{id}/preview` | ✅ | ✅ | ✅ | ❌ (401) |
| **Tải xuống SVG** | `GET /api/svg/{id}/download` | ✅ | ✅ | ✅ | ❌ (401) |
| **Xóa SVG** | `DELETE /api/svg/{id}` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Cây xe (phân trang theo hãng)** | `GET /api/vehicle-nodes?q=&page=&size=` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Tạo node xe** | `POST /api/vehicle-nodes` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Đổi tên node** | `PUT /api/vehicle-nodes/{id}` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Số liệu trước khi xoá** | `GET /api/vehicle-nodes/{id}/impact` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Xoá node + nhánh con** | `DELETE /api/vehicle-nodes/{id}` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |

### 🚗 Cây xe 4 cấp (Data Center v2 — V14)

Cây xe cố định 4 cấp theo `SA-DanhMucXe-v2` (chốt board 29/09):
1. `BRAND` — Hãng (Toyota, VinFast, Ford, Hyundai, Mazda, Kia, Abarth)
2. `SERIES` — Dòng xe (Camry, VF 8, Ranger, CX-5)
3. `MODEL` — Model (Camry 2.5Q, VF 8 Plus)
4. `SUBTYPE` — Phiên bản (Bản nhập Thái, Tiêu chuẩn) — cấp cuối, không có con

Quy tắc: BRAND không có cha; cấp con = cấp cha + 1; tên không trùng trong cùng cha
(409 `NODE_NAME_TAKEN`). `GET` phân trang theo hãng kèm cả cây con; `q` chỉ giữ node
khớp và mọi tổ tiên. `DELETE` **không chặn**: xoá cả nhánh, file chỉ mất liên kết
(`svg_file_vehicle_nodes`), file vẫn còn trong kho — trả `{deletedNodes, unlinkedFiles}`;
`GET /{id}/impact` trả `{nodes, files}` cho hộp xác nhận.

Danh mục file (`file_categories`, seed V14): Ngoại thất · Nội thất · Window film · Đèn & kính.
`svg_files` thêm `file_category_id`, `model_year` (NULL = mọi năm), `thumbnail_path`, `source`.

*(Đã gỡ mô hình cũ: `categories` 6 cấp, `car_brands`/`car_models`/`vehicle_configurations`,
phân quyền đại lý theo file `svg_file_dealer_permissions` — Q6.)*

---

## 🛡️ 5. Cơ chế khử độc SVG & Bảo mật (Security & Sanitization)

Hệ thống triển khai cơ chế kiểm tra và làm sạch nhiều lớp trước khi lưu trữ:
1. **Chống XXE (XML External Entity):**
   - Vô hiệu hóa DTDs (`disallow-doctype-decl`).
   - Tắt hoàn toàn việc tải External Entities và External DTDs qua XML parser bảo mật.
2. **Chống Stored XSS:**
   - Loại bỏ các thẻ thực thi mã: `<script>`, `<iframe>`, `<object>`, `<embed>`, `<applet>`, `<meta>`, `<link>`, v.v.
   - Loại bỏ tất cả thuộc tính sự kiện: `onload`, `onclick`, `onerror`, `onmouseover`, v.v.
   - Chặn các giao thức nguy hiểm trong thuộc tính liên kết: `javascript:`, `vbscript:`, `data:text/html`.
   - Lọc các biểu thức CSS nguy hiểm (`expression(...)`, `url(javascript:...)`).
3. **Bảo vệ Hệ thống tập tin (Path Traversal Protection):**
   - Chặn đứng các chuỗi `../`, `..\\`, `\0` trong tên file.
   - Lưu trữ với tên ngẫu nhiên UUID: `/data/svg/YYYY/MM/<uuid>.svg`.

---

## 🔑 6. Tài khoản khởi tạo mặc định (Seed Data)

| Username | Email | Mật khẩu mặc định | Vai trò (Role) | Ghi chú |
| :--- | :--- | :--- | :---: | :--- |
| `admin` | `admin@example.com` | `Password123!` | `ADMIN` | Quản trị viên tối cao hệ thống |
| `agent1` | `agent1@example.com` | `Password123!` | `AGENT` | Đại lý / Quản trị cấp 2 |
| `agent2` | `agent2@example.com` | `Password123!` | `AGENT` | Đại lý / Quản trị cấp 2 |
| `user1` | `user1@example.com` | `Password123!` | `USER` | Người dùng trực thuộc agent1 |

> **Lưu ý về đổi mật khẩu:** Người dùng có thể đổi mật khẩu trực tiếp tại Web Admin qua API `POST /api/auth/change-password`. Backend sẽ tự động đồng bộ mật khẩu mới sang Keycloak ngay lập tức.
