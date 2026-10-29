package com.example.svgmanager.integration;

import com.example.svgmanager.entity.FileCategory;
import com.example.svgmanager.entity.PartLibraryCategory;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFileVehicleNode;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserPartCategoryPermission;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.entity.VehicleNodeLevel;
import com.example.svgmanager.repository.FileCategoryRepository;
import com.example.svgmanager.repository.PartLibraryCategoryRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.SvgFileVehicleNodeRepository;
import com.example.svgmanager.repository.UserPartCategoryPermissionRepository;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
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
 * Danh mục là danh mục KHO MẪU (part_library_categories) — thứ admin gán cho thợ (bug client
 * #21/#30); thợ "kc-worker" được gán Ngoại thất + Nội thất, không được gán Phim cách nhiệt.
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
    private PartLibraryCategoryRepository partLibraryCategoryRepository;

    @Autowired
    private UserPartCategoryPermissionRepository permissionRepository;

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

    private PartLibraryCategory ngoaiThat;
    private PartLibraryCategory noiThat;
    private PartLibraryCategory phimCachNhiet;   // không gán cho thợ
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

        ngoaiThat = partLibraryCategoryRepository.save(new PartLibraryCategory("IT-NGOAI", "Ngoại thất", "ACTIVE"));
        noiThat = partLibraryCategoryRepository.save(new PartLibraryCategory("IT-NOI", "Nội thất", "ACTIVE"));
        phimCachNhiet = partLibraryCategoryRepository.save(new PartLibraryCategory("IT-PHIM", "Phim cách nhiệt", "ACTIVE"));

        User worker = userRepository.save(User.builder()
                .username("worker")
                .email("worker@test.local")
                .fullName("Thợ")
                .keycloakUserId("kc-worker")
                .password(passwordEncoder.encode("x"))
                .role(Role.USER)
                .enabled(true)
                .deleted(false)
                .build());
        permissionRepository.save(new UserPartCategoryPermission(worker, ngoaiThat, "test"));
        permissionRepository.save(new UserPartCategoryPermission(worker, noiThat, "test"));

        toyota = vehicleNodeRepository.save(new VehicleNode(null, VehicleNodeLevel.BRAND, "Toyota", 0));
        camry = vehicleNodeRepository.save(new VehicleNode(toyota, VehicleNodeLevel.SERIES, "Camry", 0));
        camry25q = vehicleNodeRepository.save(new VehicleNode(camry, VehicleNodeLevel.MODEL, "Camry 2.5Q", 0));
        banQ = vehicleNodeRepository.save(new VehicleNode(camry25q, VehicleNodeLevel.SUBTYPE, "Bản Q", 0));
        banH = vehicleNodeRepository.save(new VehicleNode(camry25q, VehicleNodeLevel.SUBTYPE, "Bản H", 1));
    }

    /** Thợ đã gán Ngoại thất + Nội thất. */
    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asWorker() {
        return jwt().jwt(j -> j.subject("kc-worker"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SvgFile saveFile(String key, PartLibraryCategory category, Integer year, String status) {
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
        file.setPartLibraryCategory(category);
        file.setModelYear(year);
        return svgFileRepository.save(file);
    }

    private void link(SvgFile file, VehicleNode node) {
        SvgFileVehicleNode link = linkRepository.save(new SvgFileVehicleNode(file, node));
        file.getVehicleNodes().add(link);
    }

    // ---------- file-categories & catalog ----------

    @Test
    @DisplayName("file-categories: thợ chỉ thấy danh mục kho mẫu được gán, theo tên (bug #21)")
    void fileCategories_workerSeesOnlyAssigned() throws Exception {
        partLibraryCategoryRepository.save(new PartLibraryCategory("IT-OLD", "Đèn cũ", "INACTIVE"));

        mockMvc.perform(get("/api/v1/file-categories")
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].value", is(String.valueOf(ngoaiThat.getId()))))
                .andExpect(jsonPath("$[0].label", is("Ngoại thất")))
                .andExpect(jsonPath("$[1].label", is("Nội thất")));
    }

    @Test
    @DisplayName("file-categories: admin thấy mọi danh mục kho mẫu đang hiệu lực")
    void fileCategories_adminSeesAllActive() throws Exception {
        partLibraryCategoryRepository.save(new PartLibraryCategory("IT-OLD", "Đèn cũ", "INACTIVE"));

        mockMvc.perform(get("/api/v1/file-categories")
                        .with(jwt().jwt(j -> j.subject("kc-content-team"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].label", hasItems("Ngoại thất", "Nội thất", "Phim cách nhiệt")))
                .andExpect(jsonPath("$[*].label", not(hasItem("Đèn cũ"))));
    }

    @Test
    @DisplayName("file-categories: user chưa có trong hệ thống → danh sách rỗng")
    void fileCategories_unknownUser_empty() throws Exception {
        mockMvc.perform(get("/api/v1/file-categories")
                        .with(jwt().jwt(j -> j.subject("kc-nobody"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("catalog: brand → con theo id cha → subtype")
    void catalog_filtersByParentId() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/brand")
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].value", is(String.valueOf(toyota.getId()))))
                .andExpect(jsonPath("$[0].label", is("Toyota")));

        mockMvc.perform(get("/api/v1/catalog/series").param("brandId", String.valueOf(toyota.getId()))
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].label", is("Camry")));

        mockMvc.perform(get("/api/v1/catalog/model").param("seriesId", String.valueOf(camry.getId()))
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].label", is("Camry 2.5Q")));

        mockMvc.perform(get("/api/v1/catalog/subtype").param("modelId", String.valueOf(camry25q.getId()))
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].label", is("Bản Q")))
                .andExpect(jsonPath("$[1].label", is("Bản H")));
    }

    @Test
    @DisplayName("catalog: thiếu id của cấp cha → 400, không tự điền giá trị mẫu")
    void catalog_missingParentId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/series")
                        .with(asWorker()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalog/model")
                        .with(asWorker()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalog/subtype")
                        .with(asWorker()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("catalog: id cha trỏ sai cấp (brandId là một SERIES) → 400")
    void catalog_wrongParentLevel_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/series").param("brandId", String.valueOf(camry.getId()))
                        .with(asWorker()))
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
                        .with(asWorker()))
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
                        .with(asWorker()))
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
                        .with(asWorker()))
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
                        .with(asWorker()))
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
                        .with(asWorker()))
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
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is("f-ngoai")));
    }

    @Test
    @DisplayName("files: thợ không thấy file thuộc danh mục chưa được gán; file chưa gắn danh mục vẫn thấy")
    void files_workerScopedToAssignedCategories() throws Exception {
        link(saveFile("f-ngoai", ngoaiThat, 2024, "ACTIVE"), camry25q);
        link(saveFile("f-phim", phimCachNhiet, 2024, "ACTIVE"), camry25q);
        link(saveFile("f-none", null, 2024, "ACTIVE"), camry25q);

        mockMvc.perform(get("/api/v1/files")
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder("f-ngoai", "f-none")));

        // Cố tình lọc theo danh mục không được gán → không lộ file nào.
        mockMvc.perform(get("/api/v1/files")
                        .param("categoryId", String.valueOf(phimCachNhiet.getId()))
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));

        mockMvc.perform(get("/api/v1/files")
                        .with(jwt().jwt(j -> j.subject("kc-content-team"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    @DisplayName("files: file cũ chỉ có file_categories vẫn hiện tên danh mục, không mang id (bug #30)")
    void files_legacyFileCategory_labelOnly() throws Exception {
        SvgFile legacy = saveFile("f-legacy", null, 2024, "ACTIVE");
        legacy.setFileCategory(fileCategoryRepository.save(new FileCategory("Window film", 9)));
        link(svgFileRepository.save(legacy), camry25q);

        mockMvc.perform(get("/api/v1/files")
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is("f-legacy")))
                .andExpect(jsonPath("$.content[0].category.value", is("")))
                .andExpect(jsonPath("$.content[0].category.label", is("Window film")));
    }

    @Test
    @DisplayName("files: id không phải số → 400")
    void files_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/files")
                        .param("categoryId", "abc")
                        .param("modelId", String.valueOf(camry25q.getId()))
                        .with(asWorker()))
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
                        .with(asWorker()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].value", is("2024")))
                .andExpect(jsonPath("$[1].value", is("2021")));
    }
}
