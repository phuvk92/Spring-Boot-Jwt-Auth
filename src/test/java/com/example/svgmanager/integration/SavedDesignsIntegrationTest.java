package com.example.svgmanager.integration;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.WorkDesign;
import com.example.svgmanager.entity.WorkDesignVersion;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.WorkDesignRepository;
import com.example.svgmanager.repository.WorkDesignVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-37 · KX-02 — GET /api/v1/designs + /api/v1/designs/{id}/versions.
 * Scope theo CHỦ SỞ HỮU (D1: bản sao riêng của thợ); rỗng ≠ lỗi; 404 ≠ mảng rỗng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SavedDesignsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkDesignRepository workDesignRepository;

    @Autowired
    private WorkDesignVersionRepository versionRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder().username("sd_user_a").email("sd_a@t.vn")
                .keycloakUserId("kc-sd-a").role(Role.USER).enabled(true).build());
        userB = userRepository.save(User.builder().username("sd_user_b").email("sd_b@t.vn")
                .keycloakUserId("kc-sd-b").role(Role.USER).enabled(true).build());
    }

    private WorkDesign design(User owner, String key, String name, LocalDateTime updatedAt) {
        WorkDesign d = new WorkDesign();
        d.setOwner(owner);
        d.setDesignKey(key);
        d.setName(name);
        d.setVehicleLabel("Abarth 695 · 2024");
        d.setCategory("Ngoại thất");
        d.setSourceTemplateId("tpl-abarth-695-2024");
        d.setSourceTemplateName("Abarth 695 2024 · Ngoại thất");
        d.setPartCount(7);
        d.setFilmUsage("6,46 m");
        d.setCutArea("1500 × 1500");
        d.setHasBeenCut(true);
        d.setLastSavedByDeviceName("MAY-XUONG-01");
        d.setCreatedAt(updatedAt.minusDays(2));
        d.setUpdatedAt(updatedAt);
        return workDesignRepository.save(d);
    }

    private WorkDesignVersion version(WorkDesign d, int number, LocalDateTime savedAt,
                                      String device, String note, boolean current) {
        WorkDesignVersion v = new WorkDesignVersion();
        v.setWorkDesign(d);
        v.setNumber(number);
        v.setSavedAt(savedAt);
        v.setSavedByDeviceName(device);
        v.setNote(note);
        v.setCurrent(current);
        return versionRepository.save(v);
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAs(String keycloakSub, String username) {
        return jwt().jwt(j -> j.subject(keycloakSub)
                        .claim("preferred_username", username))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Không có token → 401")
    void noToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/designs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/designs/wd-x/versions")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Chỉ thấy bản của chính mình, mới sửa trước, đủ metadata bảng KX-02, không lẫn mẫu kho")
    void scopedByOwner_NewestFirst() throws Exception {
        LocalDateTime t = LocalDateTime.of(2026, 9, 29, 10, 30);
        WorkDesign older = design(userA, "wd-2401", "Abarth-695-2024", t.minusDays(3));
        design(userA, "wd-2402", "Abarth-695-2024 (2)", t);
        design(userB, "wd-9999", "Bản của B", t); // không được lộ
        version(older, 2, t.minusDays(3), "MAY-XUONG-01", "Xếp lại phim", true);
        version(older, 1, t.minusDays(4), "MAY-XUONG-01", "Bản đầu tiên", false);

        mockMvc.perform(get("/api/v1/designs").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("wd-2402")))
                .andExpect(jsonPath("$[0].name", is("Abarth-695-2024 (2)")))
                .andExpect(jsonPath("$[0].vehicleLabel", is("Abarth 695 · 2024")))
                .andExpect(jsonPath("$[0].category", is("Ngoại thất")))
                .andExpect(jsonPath("$[0].partCount", is(7)))
                .andExpect(jsonPath("$[0].filmUsage", is("6,46 m")))
                .andExpect(jsonPath("$[0].cutArea", is("1500 × 1500")))
                .andExpect(jsonPath("$[0].hasBeenCut", is(true)))
                .andExpect(jsonPath("$[0].versionCount", is(0)))
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[1].id", is("wd-2401")))
                .andExpect(jsonPath("$[1].versionCount", is(2)))
                .andExpect(jsonPath("$[1].sourceTemplateId", is("tpl-abarth-695-2024")));
    }

    @Test
    @DisplayName("Chưa lưu bản nào → 200 [] — rỗng khác với lỗi (KX-35/KX-54)")
    void emptyList_NotError() throws Exception {
        design(userB, "wd-9999", "Bản của B", LocalDateTime.now());

        mockMvc.perform(get("/api/v1/designs").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("DTO chỉ có metadata — không trường hình học/path/point nào")
    void dtoHasNoGeometryFields() throws Exception {
        design(userA, "wd-2401", "Abarth-695-2024", LocalDateTime.now());

        MvcResult res = mockMvc.perform(get("/api/v1/designs").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isOk())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        for (String banned : new String[]{"geometry", "pathdata", "point", "outline", "svg", "shape"}) {
            assertThat(body.toLowerCase()).as("response không được chứa '%s'", banned).doesNotContain(banned);
        }
    }

    @Test
    @DisplayName("Bản nhập từ file ngoài (F-33): sourceTemplate null, không 500")
    void importedDesign_NullTemplate() throws Exception {
        WorkDesign d = new WorkDesign();
        d.setOwner(userA);
        d.setDesignKey("wd-2406");
        d.setName("Khach le - Ranger 2019");
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        workDesignRepository.save(d);

        mockMvc.perform(get("/api/v1/designs").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceTemplateId", nullValue()))
                .andExpect(jsonPath("$[0].sourceTemplateName", nullValue()))
                .andExpect(jsonPath("$[0].vehicleLabel", nullValue()))
                .andExpect(jsonPath("$[0].versionCount", is(0)));
    }

    @Test
    @DisplayName("Versions: đúng bản, mới nhất trước, chỉ metadata, isCurrent trên bản đầu")
    void versions_NewestFirst() throws Exception {
        LocalDateTime t = LocalDateTime.of(2026, 9, 29, 10, 30);
        WorkDesign d = design(userA, "wd-2401", "Abarth-695-2024", t);
        version(d, 1, t.minusDays(2), "MAY-XUONG-01", "Bản đầu tiên", false);
        version(d, 2, t.minusDays(1), "MAY-XUONG-02", "Nới viền capo 3mm", false);
        version(d, 3, t, "MAY-XUONG-01", null, true);
        // phiên bản của bản khác không được lẫn vào
        WorkDesign other = design(userA, "wd-2402", "Bản khác", t);
        version(other, 1, t, "MAY-XUONG-01", "Của bản khác", true);

        mockMvc.perform(get("/api/v1/designs/wd-2401/versions").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is("wd-2401-v3")))
                .andExpect(jsonPath("$[0].number", is(3)))
                .andExpect(jsonPath("$[0].isCurrent", is(true)))
                .andExpect(jsonPath("$[0].savedByDeviceName", is("MAY-XUONG-01")))
                .andExpect(jsonPath("$[0].note", nullValue()))
                .andExpect(jsonPath("$[0].savedAt").exists())
                .andExpect(jsonPath("$[1].note", is("Nới viền capo 3mm")))
                .andExpect(jsonPath("$[2].number", is(1)));
    }

    @Test
    @DisplayName("Versions: id không tồn tại → 404 DESIGN_NOT_FOUND, không trả mảng rỗng")
    void versions_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/designs/wd-khong-co/versions").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DESIGN_NOT_FOUND")));
    }

    @Test
    @DisplayName("Versions: bản của user khác → 404, không lộ tồn tại")
    void versions_otherUsersDesign_returns404() throws Exception {
        design(userB, "wd-9999", "Bản của B", LocalDateTime.now());

        mockMvc.perform(get("/api/v1/designs/wd-9999/versions").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DESIGN_NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/designs: lần Lưu đầu tạo bản làm việc và version 1 (201 Created)")
    void createDesign_Success() throws Exception {
        String json = """
                {
                    "name": "Abarth-695-2024",
                    "category": "Ngoại thất",
                    "vehicleLabel": "Abarth 695 · 2024",
                    "sourceTemplateId": "tpl-abarth-695-2024",
                    "payload": {
                        "cutArea": { "widthMm": 1500, "heightMm": 1500 },
                        "parts": [
                            { "partId": "p1", "name": "Capo", "yMm": 0, "heightMm": 1200 },
                            { "partId": "p2", "name": "Cản trước", "yMm": 1200, "heightMm": 800 }
                        ]
                    }
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/designs")
                        .with(jwtAs("kc-sd-a", "sd_user_a"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Abarth-695-2024")))
                .andExpect(jsonPath("$.category", is("Ngoại thất")))
                .andExpect(jsonPath("$.vehicleLabel", is("Abarth 695 · 2024")))
                .andExpect(jsonPath("$.sourceTemplateId", is("tpl-abarth-695-2024")))
                .andExpect(jsonPath("$.partCount", is(2)))
                .andExpect(jsonPath("$.filmUsage", is("2,00 m")))
                .andExpect(jsonPath("$.cutArea", is("1500 × 1500")))
                .andExpect(jsonPath("$.versionCount", is(1)))
                .andExpect(jsonPath("$.hasBeenCut", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/designs/{id}: các lần Lưu sau thêm version mới, tăng versionCount, không đổi sourceTemplateId")
    void updateDesign_Success() throws Exception {
        WorkDesign d = design(userA, "wd-up-1", "Bản gốc", LocalDateTime.now().minusDays(1));
        version(d, 1, LocalDateTime.now().minusDays(1), "MAY-XUONG-01", "Bản đầu", true);

        String updateJson = """
                {
                    "name": "Bản đã sửa",
                    "category": "Ngoại thất",
                    "vehicleLabel": "Abarth 695 · 2024",
                    "sourceTemplateId": "tpl-khac-khong-duoc-doi",
                    "payload": {
                        "cutArea": { "widthMm": 2000, "heightMm": 1500 },
                        "parts": [
                            { "partId": "p1", "name": "Nóc xe", "yMm": 0, "heightMm": 1500 }
                        ]
                    }
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/designs/wd-up-1")
                        .with(jwtAs("kc-sd-a", "sd_user_a"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("wd-up-1")))
                .andExpect(jsonPath("$.name", is("Bản đã sửa")))
                .andExpect(jsonPath("$.sourceTemplateId", is("tpl-abarth-695-2024"))) // Không đổi
                .andExpect(jsonPath("$.partCount", is(1)))
                .andExpect(jsonPath("$.filmUsage", is("1,50 m")))
                .andExpect(jsonPath("$.cutArea", is("2000 × 1500")))
                .andExpect(jsonPath("$.versionCount", is(2)));

        // Kiểm tra versions list
        mockMvc.perform(get("/api/v1/designs/wd-up-1/versions").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].number", is(2)))
                .andExpect(jsonPath("$[0].isCurrent", is(true)))
                .andExpect(jsonPath("$[1].number", is(1)))
                .andExpect(jsonPath("$[1].isCurrent", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/designs/{id}/content: lấy content bản hiện hành hoặc chỉ định version")
    void getContent_Success() throws Exception {
        WorkDesign d = design(userA, "wd-content-1", "Bản content", LocalDateTime.now().minusDays(1));
        WorkDesignVersion v1 = version(d, 1, LocalDateTime.now().minusDays(2), "MAY-01", "Bản 1", false);
        v1.setPayload("{\"layout\":\"version-1\"}");
        versionRepository.save(v1);

        WorkDesignVersion v2 = version(d, 2, LocalDateTime.now().minusDays(1), "MAY-01", "Bản 2", true);
        v2.setPayload("{\"layout\":\"version-2\"}");
        versionRepository.save(v2);

        // Mặc định lấy version hiện tại (v2)
        mockMvc.perform(get("/api/v1/designs/wd-content-1/content").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("wd-content-1")))
                .andExpect(jsonPath("$.version", is(2)))
                .andExpect(jsonPath("$.payload.layout", is("version-2")));

        // Chỉ định version=1
        mockMvc.perform(get("/api/v1/designs/wd-content-1/content?version=1").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("wd-content-1")))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.payload.layout", is("version-1")));
    }

    @Test
    @DisplayName("PUT hoặc GET content của user khác → 404 DESIGN_NOT_FOUND")
    void otherUserDesign_Returns404() throws Exception {
        WorkDesign d = design(userB, "wd-user-b", "Bản của B", LocalDateTime.now());
        WorkDesignVersion v = version(d, 1, LocalDateTime.now(), "MAY-01", "Bản 1", true);
        v.setPayload("{\"layout\":\"b\"}");
        versionRepository.save(v);

        String updateJson = "{\"name\":\"Sửa trộm\",\"payload\":{\"cutArea\":{\"widthMm\":100,\"heightMm\":100}}}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/designs/wd-user-b")
                        .with(jwtAs("kc-sd-a", "sd_user_a"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DESIGN_NOT_FOUND")));

        mockMvc.perform(get("/api/v1/designs/wd-user-b/content").with(jwtAs("kc-sd-a", "sd_user_a")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DESIGN_NOT_FOUND")));
    }

    @Test
    @DisplayName("Payload rỗng hoặc thiếu tên → 400 DESIGN_INVALID")
    void invalidPayload_Returns400() throws Exception {
        String noNameJson = "{\"payload\":{\"cutArea\":{\"widthMm\":100,\"heightMm\":100}}}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/designs")
                        .with(jwtAs("kc-sd-a", "sd_user_a"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(noNameJson))
                .andExpect(status().isBadRequest());

        String nullPayloadJson = "{\"name\":\"Thiếu payload\",\"payload\":null}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/designs")
                        .with(jwtAs("kc-sd-a", "sd_user_a"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(nullPayloadJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Payload vượt quá 20 MB → 413 DESIGN_TOO_LARGE")
    void payloadTooLarge_Returns413() throws Exception {
        String largeString = "a".repeat(20 * 1024 * 1024 + 10);
        String json = "{\"name\":\"Bản siêu nặng\",\"payload\":\"" + largeString + "\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/designs")
                        .with(jwtAs("kc-sd-a", "sd_user_a"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code", is("DESIGN_TOO_LARGE")));
    }
}
