package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.PartLibraryCategoryCreateRequest;
import com.example.svgmanager.dto.request.PartLibraryCategoryUpdateRequest;
import com.example.svgmanager.entity.PartLibraryCategory;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.PartLibraryCategoryRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.KeycloakUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PartLibraryCategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartLibraryCategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private User admin;
    private User regularUser;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByUsername("admin").orElseGet(() ->
                userRepository.save(User.builder().username("admin").email("admin@test.com").role(Role.ADMIN).keycloakUserId("kc-admin").build())
        );
        regularUser = userRepository.findByUsername("user1").orElseGet(() ->
                userRepository.save(User.builder().username("user1").email("user1@test.com").role(Role.USER).keycloakUserId("kc-user1").build())
        );
    }

    @Test
    @DisplayName("ADMIN tạo mới PartLibraryCategory thành công")
    void admin_createCategory_success() throws Exception {
        PartLibraryCategoryCreateRequest req = new PartLibraryCategoryCreateRequest("NEW_CAT", "Danh mục mới", "ACTIVE");

        mockMvc.perform(post("/api/part-library-categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("NEW_CAT")))
                .andExpect(jsonPath("$.name", is("Danh mục mới")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("USER tạo mới PartLibraryCategory bị 403 Forbidden")
    void user_createCategory_forbidden() throws Exception {
        PartLibraryCategoryCreateRequest req = new PartLibraryCategoryCreateRequest("TEST_FORBIDDEN", "Forbidden", "ACTIVE");

        mockMvc.perform(post("/api/part-library-categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("preferred_username", "user1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER có thể GET /api/part-library-categories/active")
    void user_canGetActiveCategories() throws Exception {
        categoryRepository.save(new PartLibraryCategory("PPF_ACTIVE", "PPF Đang chạy", "ACTIVE"));

        mockMvc.perform(get("/api/part-library-categories/active")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("preferred_username", "user1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("ADMIN cập nhật PartLibraryCategory thành công")
    void admin_updateCategory_success() throws Exception {
        PartLibraryCategory cat = categoryRepository.save(new PartLibraryCategory("UPDATABLE", "Trước cập nhật", "ACTIVE"));

        PartLibraryCategoryUpdateRequest updateReq = new PartLibraryCategoryUpdateRequest(null, "Sau cập nhật", "INACTIVE");

        mockMvc.perform(put("/api/part-library-categories/" + cat.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Sau cập nhật")))
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    @DisplayName("ADMIN xóa PartLibraryCategory chưa sử dụng thành công")
    void admin_deleteCategory_success() throws Exception {
        PartLibraryCategory cat = categoryRepository.save(new PartLibraryCategory("DELETABLE", "Để xóa", "ACTIVE"));

        mockMvc.perform(delete("/api/part-library-categories/" + cat.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin"))))
                .andExpect(status().isNoContent());
    }
}
