package com.example.svgmanager.integration;

import com.example.svgmanager.entity.FileCategory;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFileVehicleNode;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.entity.VehicleNodeLevel;
import com.example.svgmanager.repository.FileCategoryRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.SvgFileVehicleNodeRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Data Center v2 — API app thợ (SA-DanhMucXe-v2 §3.3, hợp đồng v0.6):
 * file-categories, catalog 4 cấp theo id, /api/v1/files với 4 nhánh khớp
 * (có/không subtype × có/không năm) và thiếu tham số → 400.
 *
 * Cây fixture: Toyota(BRAND) › Camry(SERIES) › Camry 2.5Q(MODEL) › {Bản Q, Bản H}(SUBTYPE).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogFilesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileCategoryRepository fileCategoryRepository;

    @Autowired
    private VehicleNodeRepository vehicleNodeRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private SvgFileVehicleNodeRepository linkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private FileCategory ngoaiThat;
    private FileCategory noiThat;
    private VehicleNode toyota;
    private VehicleNode camry;      // SERIES
    private VehicleNode camry25q;   // MODEL
    private VehicleNode banQ;       // SUBTYPE của camry25q
    private VehicleNode banH;       // SUBTYPE khác của cùng model
    private User uploader;

    @BeforeEach
    void setUp() {
        uploader = userRepository.save(User.builder()
                .username("content-team")
                .email("content@test.local")
                .fullName("Content Team")
                .keycloakUserId("kc-content-team")
                .password(passwordEncoder.encode("x"))
                .role(Role.ADMIN)
                .enabled(true)
                .deleted(false)
                .build());

        ngoaiThat = fileCategoryRepository.save(new FileCategory("Ngoại thất", 1));
        noiThat = fileCategoryRepository.save(new FileCategory("Nội thất", 2));

        toyota = vehicleNodeRepository.save(new VehicleNode(null, VehicleNodeLevel.BRAND, "Toyota", 0));
        camry = vehicleNodeRepository.save(new VehicleNode(toyota, VehicleNodeLevel.SERIES, "Camry", 0));
        camry25q = vehicleNodeRepository.save(new VehicleNode(camry, VehicleNodeLevel.MODEL, "Camry 2.5Q", 0));
        banQ = vehicleNodeRepository.save(new VehicleNode(camry25q, VehicleNodeLevel.SUBTYPE, "Bản Q", 0));
        banH = vehicleNodeRepository.save(new VehicleNode(camry25q, VehicleNodeLevel.SUBTYPE, "Bản H", 1));
    }

    private SvgFile saveFile(String key, FileCategory category, Integer year, String status) {
        SvgFile file = SvgFile.builder()
                .originalFilename(key + ".svg")
                .storedFilename("stored-" + key + ".svg")
                .filePath("/tmp/" + key + ".svg")
                .fileSize(1024L)
                .contentType("image/svg+xml")
                .status(status)
                .uploadedBy(uploader)
                .build();
        file.setFileKey(key);
        file.setDisplayName(key);
        file.setFilmUsage("1,00 m");
        file.setNote("");
        file.setFileCategory(category);
        file.setModelYear(year);
        return svgFileRepository.save(file);
    }

    private void link(SvgFile file, VehicleNode node) {
        SvgFileVehicleNode link = linkRepository.save(new SvgFileVehicleNode(file, node));
        file.getVehicleNodes().add(link);
    }

    // ---------- file-categories & catalog ----------

    @Test
    @DisplayName("file-categories: trả danh mục đang hiệu lực {value,label} theo display_order")
    void fileCategories_returnsActiveOptions() throws Exception {
        mockMvc.perform(get("/api/v1/file-categories")
                        .with(jwt().jwt(j -> j.subject("kc-user"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].value", is(String.valueOf(ngoaiThat.getId()))))
                .andExpect(jsonPath("$[0].label", is("Ngoại thất")))
                .andExpect(jsonPath("$[1].label", is("Nội thất")));
    }

    @Test
    @DisplayName("catalog: brand → con theo id cha → subtype")
    void catalog_filtersByParentId() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/brand")
                        .with(jwt().jwt(j -> j.subject("kc-user"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].value", is(String.valueOf(toyota.getId()))))
                .andExpect(jsonPath("$[0].label", is("Toyota")));

        mockMvc.perform(get("/api/v1/catalog/series").param("brandId", String.valueOf(toyota.getId()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].label", is("Camry")));

        mockMvc.perform(get("/api/v1/catalog/model").param("seriesId", String.valueOf(camry.getId()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].label", is("Camry 2.5Q")));

        mockMvc.perform(get("/api/v1/catalog/subtype").param("modelId", String.valueOf(camry25q.getId()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].label", is("Bản Q")))
                .andExpect(jsonPath("$[1].label", is("Bản H")));
    }

    @Test
    @DisplayName("catalog: thiếu id của cấp cha → 400, không tự điền giá trị mẫu")
    void catalog_missingParentId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/series")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalog/model")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalog/subtype")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("catalog: id cha trỏ sai cấp (brandId là một SERIES) → 400")
    void catalog_wrongParentLevel_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/series").param("brandId", String.valueOf(camry.getId()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());
    }

    // ---------- /api/v1/files — 4 nhánh khớp + phân trang + tham số tuỳ chọn ----------

    @Test
    @DisplayName("files: có subtype + có năm → khớp subtype hoặc model, năm NULL vẫn khớp (Q3), có vehiclePath")
    void files_withSubtypeAndYear() throws Exception {
        link(saveFile("model-2024", ngoaiThat, 2024, "ACTIVE"), camry25q); // gắn thẳng vào model
        link(saveFile("subtype-any-year", ngoaiThat, null, "ACTIVE"), banQ);
        link(saveFile("subtype-2020", ngoaiThat, 2020, "ACTIVE"), banQ);  // sai năm — không khớp

        mockMvc.perform(get("/api/v1/files")
                        .param("categoryId", String.valueOf(ngoaiThat.getId()))
                        .param("modelId", String.valueOf(camry25q.getId()))
                        .param("subtypeId", String.valueOf(banQ.getId()))
                        .param("year", "2024")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].id", is("subtype-any-year")))
                .andExpect(jsonPath("$.content[0].vehiclePath", is("Toyota › Camry › Camry 2.5Q › Bản Q")))
                .andExpect(jsonPath("$.content[1].id", is("model-2024")))
                .andExpect(jsonPath("$.content[1].category.value", is(String.valueOf(ngoaiThat.getId()))))
                .andExpect(jsonPath("$.content[1].category.label", is("Ngoại thất")))
                .andExpect(jsonPath("$.content[1].year", is(2024)))
                .andExpect(jsonPath("$.content[1].vehiclePath", is("Toyota › Camry › Camry 2.5Q")));
    }

    @Test
    @DisplayName("files: có subtype + không năm → file của subtype và của model, mọi năm")
    void files_withSubtypeNoYear() throws Exception {
        link(saveFile("f-model", ngoaiThat, 2019, "ACTIVE"), camry25q);
        link(saveFile("f-subtype", ngoaiThat, 2024, "ACTIVE"), banQ);
        link(saveFile("f-other-subtype", ngoaiThat, 2024, "ACTIVE"), banH); // phiên bản khác — không khớp
        link(saveFile("f-other-cat", noiThat, null, "ACTIVE"), banQ);       // khác danh mục — không khớp
        link(saveFile("f-deleted", ngoaiThat, 2024, "DELETED"), banQ);      // xoá mềm — không khớp

        mockMvc.perform(get("/api/v1/files")
                        .param("categoryId", String.valueOf(ngoaiThat.getId()))
                        .param("modelId", String.valueOf(camry25q.getId()))
                        .param("subtypeId", String.valueOf(banQ.getId()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].id", is("f-subtype")))
                .andExpect(jsonPath("$.content[1].id", is("f-model")));
    }

    @Test
    @DisplayName("files: không subtype + có năm → khớp model hoặc BẤT KỲ phiên bản nào của model")
    void files_noSubtypeWithYear() throws Exception {
        link(saveFile("f-model-2024", ngoaiThat, 2024, "ACTIVE"), camry25q);
        link(saveFile("f-subQ-2024", ngoaiThat, 2024, "ACTIVE"), banQ);
        link(saveFile("f-subH-null", ngoaiThat, null, "ACTIVE"), banH);
        link(saveFile("f-sub-2020", ngoaiThat, 2020, "ACTIVE"), banQ); // sai năm — không khớp

        mockMvc.perform(get("/api/v1/files")
                        .param("categoryId", String.valueOf(ngoaiThat.getId()))
                        .param("modelId", String.valueOf(camry25q.getId()))
                        .param("year", "2024")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder("f-model-2024", "f-subQ-2024", "f-subH-null")));
    }

    @Test
    @DisplayName("files: không subtype + không năm → file của model và của mọi phiên bản")
    void files_noSubtypeNoYear() throws Exception {
        link(saveFile("f-model", ngoaiThat, null, "ACTIVE"), camry25q);
        link(saveFile("f-subQ", ngoaiThat, 2021, "ACTIVE"), banQ);
        link(saveFile("f-subH", ngoaiThat, 2018, "ACTIVE"), banH);
        // Model khác cùng dòng xe — không khớp
        VehicleNode camry20g = vehicleNodeRepository.save(
                new VehicleNode(camry, VehicleNodeLevel.MODEL, "Camry 2.0G", 1));
        link(saveFile("f-other-model", ngoaiThat, 2024, "ACTIVE"), camry20g);

        mockMvc.perform(get("/api/v1/files")
                        .param("categoryId", String.valueOf(ngoaiThat.getId()))
                        .param("modelId", String.valueOf(camry25q.getId()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    @DisplayName("files: không truyền gì → mọi file ACTIVE, file DELETED không hiện")
    void files_noParams_returnsAllActive() throws Exception {
        link(saveFile("f-active-1", ngoaiThat, 2024, "ACTIVE"), camry25q);
        link(saveFile("f-active-2", noiThat, null, "ACTIVE"), banQ);
        saveFile("f-unlinked", ngoaiThat, 2023, "ACTIVE"); // file chưa gắn
        link(saveFile("f-deleted", ngoaiThat, 2024, "DELETED"), camry25q);

        mockMvc.perform(get("/api/v1/files")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[?(@.id == 'f-deleted')]").doesNotExist());
    }

    @Test
    @DisplayName("files: chỉ categoryId (không modelId) → hợp lệ, lọc đúng")
    void files_onlyCategoryId_valid() throws Exception {
        link(saveFile("f-ngoai", ngoaiThat, 2024, "ACTIVE"), camry25q);
        link(saveFile("f-noi", noiThat, 2024, "ACTIVE"), camry25q);

        mockMvc.perform(get("/api/v1/files")
                        .param("categoryId", String.valueOf(ngoaiThat.getId()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is("f-ngoai")));
    }

    @Test
    @DisplayName("files: id không phải số → 400")
    void files_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/files")
                        .param("categoryId", "abc")
                        .param("modelId", String.valueOf(camry25q.getId()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("catalog/year: chỉ trả năm có file khớp, giảm dần; file không ghi năm không sinh giá trị")
    void catalogYear_returnsYearsWithFiles() throws Exception {
        link(saveFile("f-2024", ngoaiThat, 2024, "ACTIVE"), camry25q);
        link(saveFile("f-2021", ngoaiThat, 2021, "ACTIVE"), banQ);
        link(saveFile("f-null", ngoaiThat, null, "ACTIVE"), camry25q);
        link(saveFile("f-noithat-2019", noiThat, 2019, "ACTIVE"), camry25q); // khác danh mục — không khớp

        mockMvc.perform(get("/api/v1/catalog/year")
                        .param("categoryId", String.valueOf(ngoaiThat.getId()))
                        .param("modelId", String.valueOf(camry25q.getId()))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].value", is("2024")))
                .andExpect(jsonPath("$[1].value", is("2021")));
    }
}
