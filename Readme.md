# Cutting Admin Backend REST API

Production-ready backend REST API built with **Java 21**, **Spring Boot 3.3.5**, **PostgreSQL**, **Keycloak / JWT Authentication**, and **Docker** for enterprise-grade User and SVG File Management with advanced XSS & XXE protection.

---

## 🚀 1. Giới thiệu dự án

SVG Manager Backend cung cấp một hệ sinh thái an toàn để:
- Quản lý tài khoản người dùng và phân quyền RBAC (`ADMIN`, `AGENT`, `USER`).
- Xác thực và phân quyền bằng JWT Token & Refresh Token qua Keycloak Identity Provider.
- Tải lên, xử lý khử độc (sanitization) chống mã độc XSS / XXE cho các file SVG.
- Lưu trữ file theo cấu trúc phân cấp thời gian `/data/svg/YYYY/MM/<uuid>.svg`.
- Xem trước trực tiếp (inline preview) an toàn với header bảo mật (`X-Content-Type-Options: nosniff`).
- Tải xuống file (streaming download) và tính toán mã băm SHA-256 cho mỗi file.
- Ghi log kiểm toán (Audit Logging) chuẩn hóa phục vụ giám sát vận hành.

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
│   ├── SvgController.java      # /api/svg (Upload, List, Preview, Download, Delete)
│   └── UserController.java     # /api/users (Admin CRUD, Role, Status)
├── dto
│   ├── request                 # DTO đầu vào (Login, Update, Create, Change Password)
│   └── response                # DTO đầu ra (Auth, User, Svg, Page, Error, Message)
├── entity                      # JPA Entities
│   ├── Role.java               # Enum: ADMIN, AGENT, USER
│   ├── SvgFile.java
│   └── User.java
├── exception                   # Custom Exceptions & GlobalExceptionHandler
│   ├── BadRequestException.java
│   ├── ConflictException.java
│   ├── FileStorageException.java
│   ├── ForbiddenException.java
│   ├── GlobalExceptionHandler.java
│   ├── InvalidSvgException.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
├── mapper                      # DTO Entity Mappers
│   ├── SvgMapper.java
│   └── UserMapper.java
├── repository                  # Spring Data JPA Repositories
│   ├── SvgFileRepository.java
│   └── UserRepository.java
├── security                    # Security Filters, Handlers, Converter
│   ├── CurrentUserService.java
│   ├── JwtAccessDeniedHandler.java
│   ├── JwtAuthenticationEntryPoint.java
│   └── KeycloakJwtAuthenticationConverter.java
├── service                     # Business Logic Interfaces
│   ├── AuthService.java
│   ├── FileStorageService.java
│   ├── SvgSanitizerService.java
│   ├── SvgService.java
│   └── UserService.java
│   └── impl                    # Service Implementations
├── util                        # Helper Utilities (SHA-256 Checksum, Path Traversal)
│   ├── ChecksumUtils.java
│   ├── FileUtils.java
│   └── SecurityUtils.java
└── SvgManagerApplication.java  # Main Application Class
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

Khi khởi động ứng dụng, Keycloak và Flyway migration tự động tạo tài khoản quản trị viên:

- **Username:** `admin`
- **Email:** `admin@example.com`
- **Password:** `Password123!`
- **Role:** `ADMIN`

---

## 🐳 7. Hướng dẫn chạy với Docker Compose

### Yêu cầu:
- Đã cài đặt Docker & Docker Compose.

### Khởi chạy:
```bash
# Khởi động PostgreSQL, Keycloak và Backend Service
docker compose up --build -d

# Xem log khởi động
docker compose logs -f
```

### Kiểm tra sức khỏe hệ thống:
```bash
curl http://localhost:8080/actuator/health
```

### Dừng hệ thống:
```bash
docker compose down
```

---

## 💻 8. Hướng dẫn chạy Local Development

### Yêu cầu:
- Java JDK 21 trở lên.
- PostgreSQL chạy ở `localhost:5432` với database `svg_manager`.
- Keycloak chạy ở `localhost:8180` với realm `cutting`.

### Khởi chạy ứng dụng:
```bash
./mvnw clean spring-boot:run
```

---

## 📖 9. API Documentation (Swagger UI)

Khi ứng dụng đang chạy, truy cập tài liệu API trực quan tại:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> **Cách xác thực trên Swagger:** Nhấn nút **Authorize** ở góc trên bên phải, nhập chuỗi `Bearer <accessToken>` của bạn.
