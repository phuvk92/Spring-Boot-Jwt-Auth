# PCUT - HỆ THỐNG QUẢN TRỊ (ADMIN SYSTEM)

Production-ready backend REST API built with **Java 21**, **Spring Boot 3.3.5**, **PostgreSQL**, **Keycloak / JWT Authentication**, and **Docker** for enterprise-grade User and SVG File Management with advanced XSS & XXE protection.

Thiết kế giao diện và kiến trúc tuân thủ theo chuẩn thiết kế **PCUT Admin POC**:
- **TỔNG QUAN**: Bảng tổng quan giám sát sản lượng, lượt cắt, thợ đang hoạt động và cảnh báo hệ thống *(đã loại bỏ hoàn toàn các mục Báo Cáo, Vận Hành và KINH DOANH / Doanh thu)*.
- **NỀN TẢNG & TÀI KHOẢN**: Quản lý Đại lý & Chi nhánh, Quản lý người dùng, Quản lý phiên & Thiết bị, Nhật ký quản trị (Audit Log).
- **DATA CENTER**: Quản lý danh mục xe 6 cấp bậc & Model Album, Kho mẫu & Part file (SVG), Nạp mẫu hàng loạt, Duyệt mẫu & Phân phối.

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
- Quản lý cây danh mục xe 6 cấp bậc với thuộc tính xe: Hãng xe (Brand), Dòng xe (Model), Năm sản xuất (Year).

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
│   ├── CategoryController.java # /api/categories (CRUD 6-level Tree & Vehicle Metadata)
│   ├── SvgController.java      # /api/svg (Upload, List, Preview, Download, Delete)
│   └── UserController.java     # /api/users (Admin CRUD, Role, Status)
├── dto
│   ├── request                 # DTO đầu vào (Login, Update, Create, Change Password)
│   └── response                # DTO đầu ra (Auth, User, Svg, Category, Page, Error, Message)
├── entity                      # JPA Entities
│   ├── Category.java           # Category Hierarchy + Brand/Model/Year
│   ├── Role.java               # Enum: ADMIN, AGENT, USER
│   ├── SvgFile.java
│   └── User.java
├── exception                   # Custom Exceptions & GlobalExceptionHandler
├── mapper                      # DTO Entity Mappers
├── repository                  # Spring Data JPA Repositories
│   ├── CategoryRepository.java
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
| **Cây danh mục (Catalog)** | `GET /api/categories` | ✅ | ✅ | ✅ | ❌ (401) |
| **Chi tiết danh mục** | `GET /api/categories/{id}` | ✅ | ✅ | ✅ | ❌ (401) |
| **Tạo danh mục mới** | `POST /api/categories` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Cập nhật danh mục** | `PUT /api/categories/{id}` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |
| **Xóa danh mục** | `DELETE /api/categories/{id}` | ✅ | ❌ (403) | ❌ (403) | ❌ (401) |

### 🚗 Cây danh mục xe & Thuộc tính xe (Vehicle Category Metadata)

Hệ thống quản lý cây danh mục xe phân cấp 6 tầng tự động:
1. `category` (Cấp danh mục gốc, ví dụ: "Ngoại thất", "Nội thất", "Window film")
2. `brand` (Hãng xe, ví dụ: "Toyota", "Abarth", "VinFast", "Mazda")
3. `model` (Dòng xe, ví dụ: "Camry", "695", "VF 9", "CX-5")
4. `variant` (Phiên bản, ví dụ: "2.5Q", "Signature", "Wildtrak")
5. `year` (Năm sản xuất, ví dụ: "2024", "2025", "2020-2024")
6. `submodel` (Kiểu dáng / Chi tiết, ví dụ: "Sedan 4 cửa", "Hatchback 3 cửa")

Metadata xe khi tạo (`POST /api/categories`) và cập nhật (`PUT /api/categories/{id}`):
- `brand` (String, tối đa 100 ký tự): Hãng xe
- `model` (String, tối đa 100 ký tự): Dòng xe
- `year` (String, tối đa 50 ký tự): Năm sản xuất
*(Hệ thống tự động phân giải và kế thừa thông minh nếu các trường này để trống)*

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
