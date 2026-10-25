# BÁO CÁO KIỂM TOÁN VÀ KHẮC PHỤC BẢO MẬT HỆ THỐNG CUTTING MANAGER
**Dự án:** Cutting Manager (Backend: `cutting-admin`, Frontend: `cutting-manager-web`)  
**Ngày thực hiện:** 25/10/2026  
**Vai trò thực hiện:** Application Security Engineer / Senior Security Engineer  
**Trạng thái kiểm toán:** Hoàn thành đợt kiểm tra và khắc phục tự động  

---

## 1. Tóm tắt điều hành (Executive Summary)

Đợt rà soát bảo mật toàn diện trên hai repository của hệ thống **Cutting Manager** đã hoàn tất. Quá trình kiểm toán bao gồm phân tích mã nguồn tĩnh (SAST), kiểm tra kiến trúc xác thực và phân quyền (RBAC/ABAC/IDOR), xử lý dữ liệu và tệp tin đồ họa SVG (XXE, XSS, DoS, CSS Injections), cấu hình mạng & máy chủ web (CORS, CSP, Security Headers), cơ chế mã hoá lưu trữ V2 (AES-256-GCM Envelope Encryption) và tính toàn vẹn của hệ thống ghi log kiểm toán (Audit Trail, Log Injection).

Tất cả các lỗ hổng đã xác minh trong phạm vi rủi ro từ **HIGH** đến **CRITICAL** đã được trực tiếp khắc phục an toàn trong mã nguồn, đi kèm kiểm thử đơn vị (Unit Tests) và kiểm thử tích hợp (Integration Tests) chứng minh lỗi đã được khắc phục triệt để.

### Kết quả kiểm thử tự động sau khắc phục:
- **Backend (`cutting-admin` - Spring Boot 3.3.5 / Java 21):**
  - **413 / 413 tests PASSED (0 Failures, 0 Errors, 0 Skipped)**
- **Frontend (`cutting-manager-web` - React / Vite / TypeScript):**
  - **26 / 26 vitest unit tests PASSED (4 test suites)**
  - **Build `tsc -b && vite build` THÀNH CÔNG 100% (Không phát sinh lỗi TypeScript)**

---

## 2. Bảng tổng hợp các phát hiện bảo mật (Findings Summary)

| ID | Lỗ hổng / Rủi ro bảo mật | Mức độ | Thành phần ảnh hưởng | Trạng thái |
|---|---|---|---|---|
| **SEC-01** | BOLA / IDOR & Rò rỉ phạm vi dữ liệu đại lý (Scope Leak) | **HIGH** | `DealerServiceImpl`, `DealerController` | **FIXED** (Đã khắc phục & Viết test) |
| **SEC-02** | Leo thang đặc quyền đặt lại mật khẩu qua API cập nhật người dùng | **HIGH** | `UserServiceImpl.java` | **FIXED** (Đã khắc phục & Viết test) |
| **SEC-03** | Stored XSS qua nhúng SVG inline (Thiếu CSP Sandbox) | **HIGH** | `SvgController`, `AdminUserSvgFileController`, `InternalUserSvgFileController`, `DesignFileController`, `AdminUserFilesV2Controller`, `InternalUserFilesV2Controller` | **FIXED** (Đã khắc phục & Viết test) |
| **SEC-04** | Nguy cơ lách bộ lọc SVG Sanitizer qua `foreignObject`, `style`, CSS expression, URL schemes | **HIGH** | `SvgSanitizerServiceImpl.java` | **FIXED** (Đã khắc phục & Viết test) |
| **SEC-05** | CORS cấu hình `allowedOriginPatterns("*")` kết hợp `allowCredentials(true)` | **MEDIUM** | `SecurityConfig.java` | **FIXED** (Đã cấu hình whitelisting) |
| **SEC-06** | Cấu hình `permitAll` quá rộng trên đường dẫn `/api/auth/**` | **MEDIUM** | `SecurityConfig.java` | **FIXED** (Thu hẹp quyền truy cập endpoint) |
| **SEC-07** | Thiếu các HTTP Security Headers thiết yếu (Clickjacking, MIME Sniffing, Referrer) | **MEDIUM** | `SecurityConfig.java` | **FIXED** (Đã cấu hình FrameOptions, NoSniff, ReferrerPolicy) |
| **SEC-08** | Nguy cơ Log Injection & Giả mạo IP qua `X-Forwarded-For` | **LOW** | `InternalApiV2AuditFilter.java` | **FIXED** (Bổ sung regex sanitization `SAFE_IP_PATTERN`) |

---

## 3. Phân tích kỹ thuật chi tiết từng phát hiện & Giải pháp khắc phục

### 3.1. SEC-01: BOLA / IDOR & Scope Leak trong Quản lý Đại lý (Dealer Management)
- **CWE:** CWE-639 (Authorization Bypass Through User-Controlled Key / BOLA)
- **Mức độ rủi ro:** **HIGH**
- **Mô tả lỗ hổng:**
  - API `GET /api/dealers/{id}` trước đây cho phép bất kỳ người dùng có vai trò `AGENT` hoặc `USER` tra cứu thông tin chi tiết của các đại lý khác chỉ bằng việc thay đổi tham số `{id}` trên đường dẫn.
  - API `GET /api/dealers` và `GET /api/dealers/all` không lọc theo đại lý của AGENT khi người gọi là AGENT, dẫn tới việc lộ toàn bộ danh sách đại lý của hệ thống.
  - API `GET /api/dealers/stats` cho phép AGENT truy cập thống kê toàn hệ thống.
- **Giải pháp khắc phục:**
  1. Trong `DealerServiceImpl.getDealerById(Long id)`: Kiểm tra `currentUserService.getCurrentUser()`. Nếu người dùng không phải `ADMIN` và có gán `dealer`, nếu `{id}` khác `dealer.getId()` thì lập tức từ chối và ném `ForbiddenException("Không có quyền xem thông tin của đại lý khác")`.
  2. Trong `DealerServiceImpl.getDealers(...)`: Thêm điều kiện scoping vào JPA `Specification`. Nếu `isNotAdmin` và có `agentDealerId`, thêm predicate `cb.equal(root.get("id"), agentDealerId)`.
  3. Trong `DealerServiceImpl.getAllDealers()`: Nếu người gọi là non-admin, chỉ trả về đại lý duy nhất thuộc quyền quản lý của họ.
  4. Trong `DealerController.getDealerStats()`: Khóa quyền truy cập bằng `@PreAuthorize("hasRole('ADMIN')")`.
- **Minh chứng kiểm thử:**
  - `SecurityAuditRemediationIntegrationTest`:
    - `agent_GetOtherDealerById_ReturnsForbidden`: 403 Forbidden.
    - `agent_GetOwnDealerById_ReturnsOk`: 200 OK.
    - `agent_GetDealersList_ScopedToOwnDealer`: Chỉ trả về 1 đại lý của chính AGENT.
    - `agent_GetAllDealers_ScopedToOwnDealer`: Chỉ trả về 1 đại lý của chính AGENT.
    - `agent_GetDealerStats_ReturnsForbidden`: 403 Forbidden.
    - `admin_GetDealerStats_ReturnsOk`: 200 OK.

---

### 3.2. SEC-02: Nguy cơ leo thang đặc quyền đặt lại mật khẩu qua `updateUser`
- **CWE:** CWE-269 (Improper Privilege Management)
- **Mức độ rủi ro:** **HIGH**
- **Mô tả lỗ hổng:**
  - `UpdateUserRequest` chứa trường `password`. Nếu một AGENT hoặc tài khoản quản trị cấp thấp có quyền gọi `updateUser` sửa thông tin người dùng cấp dưới, họ có thể truyền trường `password` vào payload để ép đổi mật khẩu của người khác mà không thông qua quy trình phê duyệt mật khẩu của quản trị viên hệ thống.
- **Giải pháp khắc phục:**
  - Trong `UserServiceImpl.updateUser(Long id, UpdateUserRequest request)`: Bổ sung kiểm tra xác thực quyền:
    ```java
    if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền đặt lại mật khẩu cho người dùng khác");
        }
    }
    ```
- **Minh chứng kiểm thử:**
  - `SecurityAuditRemediationIntegrationTest.nonAdmin_UpdateUser_WithPassword_ReturnsForbidden`: AGENT gọi `PUT /api/users/{id}` kèm mật khẩu mới nhận mã phản hồi **403 Forbidden**.

---

### 3.3. SEC-03: Stored XSS qua luồng xem trước SVG inline (Missing CSP Sandbox)
- **CWE:** CWE-79 (Improper Neutralization of Input During Web Page Generation / Stored XSS)
- **Mức độ rủi ro:** **HIGH**
- **Mô tả lỗ hổng:**
  - Các endpoint xem trước file SVG (V1 và V2) trả về `image/svg+xml`. Nếu trình duyệt mở trực tiếp file SVG hoặc render SVG qua thẻ `<object>` / `<iframe>`, bất kỳ mã script nào nằm trong SVG (hoặc bypass được bộ lọc) có thể thực thi trong context của ứng dụng web, dẫn đến đánh cắp session/token.
- **Giải pháp khắc phục:**
  - Thiết lập nghiêm ngặt tiêu đề `Content-Security-Policy` và `X-Content-Type-Options: nosniff` trên tất cả các response stream SVG inline (`SvgController`, `AdminUserSvgFileController`, `InternalUserSvgFileController`, `DesignFileController`, `AdminUserFilesV2Controller`, `InternalUserFilesV2Controller`):
    ```http
    Content-Security-Policy: default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'; sandbox
    X-Content-Type-Options: nosniff
    ```
  - Chỉ thị `sandbox` (không có `allow-scripts` hay `allow-same-origin`) đảm bảo trình duyệt cô lập hoàn toàn SVG, cấm thực thi bất kỳ JavaScript nào.
- **Minh chứng kiểm thử:**
  - `InternalUserFilesV2IntegrationTest.testPreviewAndDownload_TransparentDecryption` và `InternalUserFilesV2SecurityTest.testAccept_ValidCuttingSvg_PreservesGeometry`: Xác nhận header `Content-Security-Policy` chứa `sandbox` được gửi kèm trong response 200 OK.

---

### 3.4. SEC-04: Tăng cường bộ lọc làm sạch SVG (SVG Sanitizer Hardening)
- **CWE:** CWE-79 (XSS) / CWE-611 (XXE Injection)
- **Mức độ rủi ro:** **HIGH**
- **Mô tả lỗ hổng:**
  - Danh sách thẻ cấm của `SvgSanitizerServiceImpl` trước đây chưa bao quát các thẻ vector động có thể kích hoạt JavaScript hoặc HTML embedding như `<foreignObject>`, `<animate>`, `<set>`, `<animatemotion>`, `<animatetransform>`, `<handler>`, `<listener>`.
  - Khối `<style>` có thể bị lợi dụng để chèn `expression(...)` (CSS expression), `@import` từ domain lạ hoặc URL giả mạo.
  - Các thuộc tính `href`, `xlink:href`, `src` có thể dẫn ra các giao thức nguy hiểm (`javascript:`, `data:`, `file:`).
- **Giải pháp khắc phục:**
  1. Mở rộng `FORBIDDEN_TAGS`: Bổ sung `foreignObject`, `animate`, `set`, `animatemotion`, `animatetransform`, `handler`, `listener`, `audio`, `video`.
  2. Bổ sung làm sạch CSS trong `<style>`: Sử dụng `element.wholeText()` (đáp ứng tương thích với Jsoup XML Parser) để kiểm tra các mẫu độc hại như `expression(`, `@import`, `url(javascript:`, `url(data:`, `url(file:`. Nếu vi phạm, loại bỏ thẻ `<style>` nguy hiểm.
  3. Bổ sung kiểm tra `href`, `xlink:href`, `src`: Loại bỏ các URL bắt đầu bằng `javascript:`, `data:`, `file:`, `blob:`, `http:`, `https:`, chỉ cho phép neo nội bộ `#id` (dành cho SVG gradients, clip-paths, symbols).
  4. Tinh chỉnh `MALICIOUS_XXE_PATTERNS`: Ngăn chặn `SYSTEM`, `PUBLIC` trỏ tới file/http và các thực thể tham số `%`, đồng thời vẫn tương thích hoàn toàn với DOCTYPE tiêu chuẩn xuất khẩu từ Adobe Illustrator và CorelDRAW.
- **Minh chứng kiểm thử:**
  - `SvgSanitizerServiceTest`: Đã bổ sung 4 test case bảo mật chuyên sâu:
    - `sanitize_RemoveForeignObject`: Đảm bảo `foreignObject` bị loại bỏ.
    - `sanitize_RemoveDangerousStyleBlock`: Đảm bảo CSS expression / @import bị chặn.
    - `sanitize_RemoveAnimationTags`: Đảm bảo `animate`, `set` bị dọn dẹp.
    - `sanitize_RemoveExternalHrefs`: Đảm bảo URL bên ngoài hoặc javascript href bị xóa.
    - Toàn bộ 17 tests của `SvgSanitizerServiceTest` đã PASS.

---

### 3.5. SEC-05: Cấu hình CORS an toàn (Insecure CORS Wildcard with Credentials)
- **CWE:** CWE-942 (Permissive Cross-Domain Policy with Untrusted Domains)
- **Mức độ rủi ro:** **MEDIUM**
- **Mô tả lỗ hổng:**
  - `SecurityConfig.java` trước đây cấu hình:
    ```java
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowCredentials(true);
    ```
  - Cấu hình này mở quyền CORS cho mọi nguồn gốc kết hợp với cookie/credentials, tiềm ẩn nguy cơ khai thác Cross-Site Request qua browser của nạn nhân.
- **Giải pháp khắc phục:**
  - Thay thế bằng danh sách origin an toàn có thể cấu hình linh hoạt qua biến môi trường hoặc file thuộc tính `app.cors.allowed-origins` (mặc định cho môi trường phát triển nội bộ: `http://localhost:3000`, `http://localhost:8080`, `http://localhost:5173`, `http://127.0.0.1:3000`, `http://127.0.0.1:5173`).
- **Minh chứng kiểm thử:**
  - Tất cả các Integration Tests qua Spring Security Filter Chain đều chạy thành công.

---

### 3.6. SEC-06 & SEC-07: Thu hẹp `permitAll` trên Auth & Bổ sung HTTP Security Headers
- **CWE:** CWE-306 (Missing Authentication for Critical Function) / CWE-1021 (Improper Restriction of Rendered UI Layers or Frames)
- **Mức độ rủi ro:** **MEDIUM**
- **Mô tả lỗ hổng:**
  - Quy tắc `.requestMatchers("/api/auth/**").permitAll()` cho phép truy cập mà không kiểm tra xác thực ở tầng filter vào các endpoint như `/api/auth/me`, `/api/auth/change-password`.
  - Thiếu các HTTP Security Headers mặc định: `X-Frame-Options` (bảo vệ chống Clickjacking), `X-Content-Type-Options` (bảo vệ chống MIME sniffing), `Referrer-Policy`.
- **Giải pháp khắc phục:**
  1. Giới hạn `permitAll` chỉ cho các endpoint công khai: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/refresh-token`.
  2. Các endpoint còn lại trong `/api/auth/**` (bao gồm `/api/auth/me`, `/api/auth/change-password`) được đưa về diện yêu cầu xác thực (`authenticated()`).
  3. Bổ sung cấu hình headers trên Security Filter Chain:
     - `headers.frameOptions(frame -> frame.sameOrigin())`
     - `headers.contentTypeOptions(Customizer.withDefaults())`
     - `headers.xssProtection(Customizer.withDefaults())`
     - `headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))`
- **Minh chứng kiểm thử:**
  - `SecurityAuditRemediationIntegrationTest.unauthenticated_GetAuthMe_ReturnsUnauthorized`: Không có token trả về 401 Unauthorized.
  - `SecurityAuditRemediationIntegrationTest.securityHeaders_PresentOnResponses`: Header `X-Content-Type-Options: nosniff` và `X-Frame-Options: SAMEORIGIN` xuất hiện trong response.

---

### 3.7. SEC-08: Ngăn ngừa Log Injection & Giả mạo IP trong Audit Filter
- **CWE:** CWE-117 (Improper Output Handling for Logs)
- **Mức độ rủi ro:** **LOW**
- **Mô tả lỗ hổng:**
  - `InternalApiV2AuditFilter` đọc tiêu đề `X-Forwarded-For` và ghi nhận trực tiếp vào cơ sở dữ liệu kiểm toán và log file mà chưa làm sạch triệt để, có thể bị đối tượng tấn công chèn ký tự điều khiển (`\r\n`) để giả mạo dòng log.
- **Giải pháp khắc phục:**
  - Thêm regex whitelist `SAFE_IP_PATTERN = Pattern.compile("^[a-fA-F0-9:.]+$")`.
  - Nếu IP chứa ký tự không hợp lệ hoặc vượt quá 45 ký tự (độ dài tối đa IPv6/IPv4-mapped), hệ thống tự động loại bỏ và chuyển về fallback `request.getRemoteAddr()`.
- **Minh chứng kiểm thử:**
  - `InternalApiV2AuditIntegrationTest` và `AuditTrailIntegrationTest`: Toàn bộ 12 tests đã PASS thành công.

---

## 4. Danh sách các tệp tin đã chỉnh sửa (Files Changed)

### Backend (`/Users/phuvk/IdeaProjects/Cutting-Admin`):
1. `src/main/java/com/example/svgmanager/config/SecurityConfig.java`: Cấu hình CORS an toàn, thu hẹp quyền endpoint auth, bổ sung FrameOptions, ContentTypeOptions, ReferrerPolicy.
2. `src/main/java/com/example/svgmanager/controller/DealerController.java`: Giới hạn `getDealerStats` cho ADMIN (`@PreAuthorize("hasRole('ADMIN')")`).
3. `src/main/java/com/example/svgmanager/service/impl/DealerServiceImpl.java`: Thực thi phân quyền BOLA/IDOR và scoping danh sách đại lý cho AGENT.
4. `src/main/java/com/example/svgmanager/service/impl/UserServiceImpl.java`: Chặn non-admin thay đổi mật khẩu người khác qua `updateUser`.
5. `src/main/java/com/example/svgmanager/service/impl/SvgSanitizerServiceImpl.java`: Mở rộng danh sách cấm, lọc CSS expression/style, làm sạch URL scheme, tăng cường chặn XXE.
6. `src/main/java/com/example/svgmanager/security/InternalApiV2AuditFilter.java`: Bổ sung kiểm tra regex `SAFE_IP_PATTERN` cho IP client trong audit trail.
7. `src/main/java/com/example/svgmanager/controller/SvgController.java`: Bổ sung CSP sandbox header cho preview SVG.
8. `src/main/java/com/example/svgmanager/controller/AdminUserSvgFileController.java`: Bổ sung CSP sandbox header cho admin preview SVG.
9. `src/main/java/com/example/svgmanager/controller/internal/InternalUserSvgFileController.java`: Bổ sung CSP sandbox header cho internal preview SVG.
10. `src/main/java/com/example/svgmanager/controller/DesignFileController.java`: Bổ sung CSP sandbox header cho design file preview SVG.
11. `src/main/java/com/example/svgmanager/controller/admin/v2/AdminUserFilesV2Controller.java`: Thống nhất CSP sandbox header cho preview SVG V2.
12. `src/main/java/com/example/svgmanager/controller/internal/v2/InternalUserFilesV2Controller.java`: Thống nhất CSP sandbox header cho preview SVG V2.
13. `src/test/java/com/example/svgmanager/service/SvgSanitizerServiceTest.java`: Thêm 4 test case bảo mật SVG.
14. `src/test/java/com/example/svgmanager/integration/SecurityAuditRemediationIntegrationTest.java`: Tạo mới test suite kiểm thử hồi quy bảo mật (BOLA, Privilege Escalation, Auth, Headers).
15. `src/test/java/com/example/svgmanager/integration/InternalUserFilesV2IntegrationTest.java`: Thêm kiểm tra CSP sandbox.
16. `src/test/java/com/example/svgmanager/integration/InternalUserFilesV2SecurityTest.java`: Cập nhật kỳ vọng CSP header.
17. `src/test/java/com/example/svgmanager/integration/VehicleNodesPostgresIntegrationTest.java`: Dọn dẹp dữ liệu test đảm bảo tính độc lập.

---

## 5. Kết quả chạy kiểm thử (Test Execution Evidence)

### 5.1. Backend Test Suite (`./mvnw test`)
```
[INFO] Results:
[INFO] Tests run: 413, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  30.972 s
[INFO] Finished at: 2026-10-25T23:20:00+07:00
```

### 5.2. Frontend Test Suite (`vitest run`)
```
 Test Files  4 passed (4)
      Tests  26 passed (26)
   Start at  23:20:13
   Duration  163ms
```

### 5.3. Frontend Build (`tsc -b && vite build`)
```
✓ 3322 modules transformed.
✓ built in 428ms
dist/index.html   1.14 kB │ gzip: 0.56 kB
...
```

---

## 6. Rủi ro còn tồn tại & Khuyến nghị triển khai Production (Residual Risks & Recommendations)

1. **Khóa mã hóa V2 (V2 Master Encryption Key):**
   - Lớp `MasterKeyEncryptionKeyV2ServiceImpl` hiện có fallback sử dụng khóa phát triển khi biến môi trường `V2_MASTER_KEY` không được cung cấp.
   - *Khuyến nghị:* Trên môi trường Production, bắt buộc phải inject biến môi trường `V2_MASTER_KEY` thông qua hệ thống quản lý bí mật (HashiCorp Vault, AWS Secrets Manager hoặc Docker Secrets) với độ dài tối thiểu 256 bits dạng base64/hex.
2. **Cấu hình Nginx / Reverse Proxy Production:**
   - Cần đảm bảo Nginx reverse proxy thiết lập đúng tiêu đề `X-Forwarded-Proto: https` và chỉ định cấu hình `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;` từ các IP proxy đáng tin cậy để tránh giả mạo client IP.
3. **Quản lý Secrets và Phân quyền Keycloak Realm:**
   - Đảm bảo client secret giữa Backend và Keycloak realm `cutting-manager` được bảo mật an toàn, kích hoạt cơ chế xoay vòng mật khẩu (Secret Rotation) định kỳ.
4. **Giới hạn kích thước tệp SVG:**
   - Hệ thống hiện tại đã giới hạn kích thước upload; khuyến nghị duy trì giới hạn tối đa không quá 20MB để ngăn ngừa tấn công từ chối dịch vụ (DoS / Zip-bomb / XML expansion) gây tiêu tốn tài nguyên CPU khi phân tích cú pháp.
