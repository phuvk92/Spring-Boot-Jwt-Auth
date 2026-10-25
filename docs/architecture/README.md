# 📐 Tài liệu Kiến trúc & Flow Hệ thống PCUT (PlantUML & Diagrams)

Thư mục này chứa toàn bộ tài liệu kiến trúc hệ thống và biểu đồ luồng hoạt động (Sequence Flows) của hệ sinh thái **PCUT Admin & Cutting Engine**, được mô tả bằng định dạng **PlantUML** chuẩn (`.puml`) sẵn sàng để lưu trữ và version control trên GitHub.

---

## 🗂️ Danh mục các file PlantUML

| Tên File | Loại Biểu đồ | Mô tả chi tiết |
| :--- | :--- | :--- |
| **[`system-architecture.puml`](./system-architecture.puml)** | Component / Architecture | Kiến trúc tổng thể toàn bộ hệ thống: Web Admin, App máy cắt thợ, Nginx, Spring Boot Backend (Filter Chain, Controllers, Service Engines), Keycloak IAM, PostgreSQL 16 và Storage Volume. |
| **[`flow-auth-security.puml`](./flow-auth-security.puml)** | Sequence Flow | Luồng xác thực & ủy quyền: Đăng nhập OAuth2 / OpenID Connect Keycloak, cấp phát JWT / Offline Token (Remember Me), kiểm soát thiết bị (Device Session Tracking) và RBAC. |
| **[`flow-svg-upload-processing.puml`](./flow-svg-upload-processing.puml)** | Sequence Flow | Luồng nạp file mẫu SVG: Khử độc chống Stored XSS / XXE (Jsoup DOM), bóc tách Part & tọa độ hình học vector (`SvgImport`), lưu trữ file và gắn liên kết cây xe 4 cấp. |
| **[`flow-internal-v2-encrypted.puml`](./flow-internal-v2-encrypted.puml)** | Sequence Flow | Phân hệ Internal API V2: Lưu trữ bản vẽ người dùng mã hóa đa tầng Envelope Encryption (AES-256-GCM), cơ chế ghi vết Zero-Intrusive Audit Log ($O(1)$ memory) và chia sẻ bản vẽ. |
| **[`flow-cutter-catalog-v1.puml`](./flow-cutter-catalog-v1.puml)** | Sequence Flow | API app thợ máy cắt (`/api/v1/*`): Tra cứu cây xe 4 cấp (`BRAND › SERIES › MODEL › SUBTYPE`), lọc danh mục, tìm kiếm file và tải dữ liệu hình học vector để điều khiển dao cắt PPF/decal. |

---

## 🖥️ Hướng dẫn xem & xuất hình ảnh PlantUML

### 1. Xem trực tiếp trong IDE
- **IntelliJ IDEA:** Cài plugin **PlantUML Integration** từ JetBrains Marketplace. Mở bất kỳ file `.puml` nào, cửa sổ preview sẽ hiển thị tự động bên phải.
- **VS Code:** Cài extension **PlantUML** của tác giả *jebbs*. Nhấn `Alt + D` (hoặc `Option + D` trên Mac) để xem preview.

### 2. Xuất ra ảnh PNG / SVG bằng Command Line
Nếu máy tính đã cài đặt PlantUML (`brew install plantuml` hoặc dùng Docker):
```bash
# Xuất tất cả các biểu đồ sang định dạng PNG
plantuml docs/architecture/*.puml -tpng

# Hoặc xuất sang định dạng SVG (vector sắc nét)
plantuml docs/architecture/*.puml -tsvg
```

### 3. Xem trực tuyến (Online)
Bạn có thể copy nội dung bất kỳ file `.puml` nào và dán vào [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/) để xem hoặc chia sẻ liên kết tức thì.

---

## 📊 Xem nhanh Biểu đồ Kiến trúc (GitHub Mermaid Native Preview)

> *Biểu đồ dưới đây được render tự động ngay trên giao diện web của GitHub bằng cú pháp Mermaid:*

```mermaid
flowchart TD
    subgraph Clients["1. Presentation Tier (Clients)"]
        Admin["Quản trị viên / Đại lý<br/>(Admin & Agent)"]
        Cutter["Kỹ thuật viên / Thợ cắt<br/>(Cutter Operator)"]
        WebAdmin["Web Admin Portal<br/>(React / Vite - Port 3000)"]
        CutterApp["App Máy cắt PPF / Decal<br/>(DesignModule - C# .NET)"]
        Admin --> WebAdmin
        Cutter --> CutterApp
    end

    subgraph Security["2. Ingress & Identity Provider"]
        Nginx["Nginx Reverse Proxy<br/>Port 3000 -> 80"]
        Keycloak["Keycloak 25 (IAM)<br/>Port 8180 (Realm: cutting)<br/>- OAuth2 / JWT / Offline Token<br/>- Roles: ADMIN, AGENT, USER"]
        WebAdmin --> Nginx
        Nginx --> Keycloak
        CutterApp --> Keycloak
    end

    subgraph Backend["3. Backend Core (Spring Boot 3.3.5 - Port 8080)"]
        subgraph Filters["Filter & Security Chain"]
            AuditFilter["InternalApiV2AuditFilter<br/>(X-Request-ID, O(1) Stream)"]
            DeviceFilter["DeviceSessionFilter<br/>(Kiểm tra thiết bị khóa/thu hồi)"]
            JwtConverter["KeycloakJwtConverter<br/>(Xác thực chữ ký số JWK Set)"]
            AuditFilter --> DeviceFilter --> JwtConverter
        end

        subgraph Controllers["REST Controllers"]
            AuthCtrl["Auth & User APIs<br/>/api/auth, /api/users"]
            VehicleCtrl["Vehicle Node APIs<br/>/api/vehicle-nodes (Cây xe 4 cấp)"]
            SvgCtrl["SVG & Admin APIs<br/>/api/svg, /api/admin/files"]
            CutterCtrl["Cutter APIs (v1)<br/>/api/v1/catalog, /api/v1/files"]
            InternalV2Ctrl["Internal API V2<br/>/api/internal/v2/user-files"]
        end

        subgraph Engines["Core Processing Engines"]
            Sanitizer["SVG Sanitizer Engine<br/>(Jsoup DOM: Chống XSS & XXE)"]
            SvgImport["SVG Geometry Engine<br/>(Tách Part, Bounding Box, Path Data)"]
            EncryptV2["Envelope Encryption V2<br/>(AES-256-GCM + DEK/KEK v1)"]
            AuditSvc["Audit Log Service V2<br/>(Async logging & CSV stats)"]
        end

        JwtConverter --> Controllers
        SvgCtrl --> Sanitizer --> SvgImport
        InternalV2Ctrl --> EncryptV2
        AuditFilter -.-> AuditSvc
    end

    subgraph Persistence["4. Persistence & Storage Tier"]
        DB[("PostgreSQL 16 (svg-postgres:5432)<br/>- vehicle_nodes (4 cấp)<br/>- svg_files, svg_parts, geometries<br/>- user_svg_files, user_file_shares<br/>- audit_logs_v2")]
        Storage[("Docker Volume Storage (/data/svg)<br/>- /YYYY/MM/<uuid>.svg (Mẫu gốc)<br/>- /v2/user-files/<uuid>.enc (Mã hóa)")]
    end

    Nginx -->|Proxy API| AuditFilter
    CutterApp -->|Direct API| AuditFilter
    Controllers --> DB
    Engines --> DB
    SvgCtrl --> Storage
    EncryptV2 --> Storage
```
