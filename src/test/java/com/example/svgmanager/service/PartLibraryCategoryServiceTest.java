package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.PartLibraryCategoryCreateRequest;
import com.example.svgmanager.dto.request.PartLibraryCategoryUpdateRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.PartLibraryCategoryResponse;
import com.example.svgmanager.entity.PartLibraryCategory;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.PartLibraryCategoryRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.impl.PartLibraryCategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartLibraryCategoryServiceTest {

    @Mock
    private PartLibraryCategoryRepository categoryRepository;

    @Mock
    private SvgFileRepository svgFileRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PartLibraryCategoryServiceImpl service;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .role(Role.ADMIN)
                .build();
        lenient().when(currentUserService.getCurrentUser()).thenReturn(adminUser);
    }

    @Test
    @DisplayName("Tạo mới danh mục kho mẫu & part thành công, mã được uppercase")
    void createCategory_success() {
        PartLibraryCategoryCreateRequest request = new PartLibraryCategoryCreateRequest("interior", "Nội thất xe hơi", "ACTIVE");
        when(categoryRepository.existsByCode("INTERIOR")).thenReturn(false);

        PartLibraryCategory saved = new PartLibraryCategory("INTERIOR", "Nội thất xe hơi", "ACTIVE");
        saved.setId(10L);
        saved.setCreatedAt(LocalDateTime.now());
        when(categoryRepository.save(any(PartLibraryCategory.class))).thenReturn(saved);
        when(svgFileRepository.countByPartLibraryCategory_Id(10L)).thenReturn(0L);

        PartLibraryCategoryResponse response = service.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.code()).isEqualTo("INTERIOR");
        assertThat(response.name()).isEqualTo("Nội thất xe hơi");
        assertThat(response.status()).isEqualTo("ACTIVE");

        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("PART_LIBRARY_CATEGORY_CREATED"),
                eq("PartLibraryCategory"), eq(10L), contains("INTERIOR"));
    }

    @Test
    @DisplayName("Tạo mới danh mục bị từ chối nếu trùng mã code")
    void createCategory_duplicateCode_throwsBadRequest() {
        PartLibraryCategoryCreateRequest request = new PartLibraryCategoryCreateRequest("EXTERIOR", "Ngoại thất", "ACTIVE");
        when(categoryRepository.existsByCode("EXTERIOR")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mã danh mục kho mẫu & part đã tồn tại: EXTERIOR");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật danh mục thành công và ghi log kích hoạt khi đổi sang ACTIVE")
    void updateCategory_success_statusActivated() {
        PartLibraryCategory existing = new PartLibraryCategory("EXTERIOR", "Ngoại thất", "INACTIVE");
        existing.setId(2L);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(existing));

        when(categoryRepository.save(any(PartLibraryCategory.class))).thenAnswer(i -> i.getArgument(0));
        when(svgFileRepository.countByPartLibraryCategory_Id(2L)).thenReturn(3L);

        PartLibraryCategoryUpdateRequest request = new PartLibraryCategoryUpdateRequest(null, "Ngoại thất cao cấp", "ACTIVE");
        PartLibraryCategoryResponse response = service.updateCategory(2L, request);

        assertThat(response.name()).isEqualTo("Ngoại thất cao cấp");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.usageCount()).isEqualTo(3L);

        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("PART_LIBRARY_CATEGORY_ACTIVATED"),
                eq("PartLibraryCategory"), eq(2L), anyString());
        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("PART_LIBRARY_CATEGORY_UPDATED"),
                eq("PartLibraryCategory"), eq(2L), anyString());
    }

    @Test
    @DisplayName("Cập nhật danh mục: ghi log hủy kích hoạt khi đổi sang INACTIVE")
    void updateCategory_statusDeactivated() {
        PartLibraryCategory existing = new PartLibraryCategory("EXTERIOR", "Ngoại thất", "ACTIVE");
        existing.setId(2L);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(PartLibraryCategory.class))).thenAnswer(i -> i.getArgument(0));

        PartLibraryCategoryUpdateRequest request = new PartLibraryCategoryUpdateRequest(null, null, "INACTIVE");
        service.updateCategory(2L, request);

        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("PART_LIBRARY_CATEGORY_DEACTIVATED"),
                eq("PartLibraryCategory"), eq(2L), anyString());
    }

    @Test
    @DisplayName("Cập nhật danh mục: đổi mã code bị trùng thì báo lỗi BadRequest")
    void updateCategory_duplicateCode_throwsBadRequest() {
        PartLibraryCategory existing = new PartLibraryCategory("EXTERIOR", "Ngoại thất", "ACTIVE");
        existing.setId(2L);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByCodeAndIdNot("WINDOW_FILM", 2L)).thenReturn(true);

        PartLibraryCategoryUpdateRequest request = new PartLibraryCategoryUpdateRequest("WINDOW_FILM", null, null);

        assertThatThrownBy(() -> service.updateCategory(2L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mã danh mục kho mẫu & part đã tồn tại: WINDOW_FILM");
    }

    @Test
    @DisplayName("Xóa danh mục thành công khi chưa có part file nào sử dụng")
    void deleteCategory_notUsed_success() {
        PartLibraryCategory category = new PartLibraryCategory("SUNROOF", "Cửa sổ trời", "ACTIVE");
        category.setId(5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(svgFileRepository.countByPartLibraryCategory_Id(5L)).thenReturn(0L);

        service.deleteCategory(5L);

        verify(categoryRepository).delete(category);
        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("PART_LIBRARY_CATEGORY_DELETED"),
                eq("PartLibraryCategory"), eq(5L), contains("SUNROOF"));
    }

    @Test
    @DisplayName("Xóa danh mục bị chặn khi đang có part file sử dụng")
    void deleteCategory_inUse_throwsBadRequest() {
        PartLibraryCategory category = new PartLibraryCategory("INTERIOR", "Nội thất", "ACTIVE");
        category.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(svgFileRepository.countByPartLibraryCategory_Id(1L)).thenReturn(8L);

        assertThatThrownBy(() -> service.deleteCategory(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể xóa danh mục đang có 8 part file sử dụng");

        verify(categoryRepository, never()).delete(any(PartLibraryCategory.class));
        verify(auditLogService, never()).log(any(), any(), eq("PART_LIBRARY_CATEGORY_DELETED"), any(), any(), any());
    }

    @Test
    @DisplayName("Lấy danh mục active cho dropdown chỉ trả các danh mục có status ACTIVE")
    void getActiveCategories_returnsOnlyActive() {
        PartLibraryCategory c1 = new PartLibraryCategory("INTERIOR", "Nội thất", "ACTIVE");
        c1.setId(1L);
        PartLibraryCategory c2 = new PartLibraryCategory("EXTERIOR", "Ngoại thất", "ACTIVE");
        c2.setId(2L);
        when(categoryRepository.findByStatusOrderByNameAsc("ACTIVE")).thenReturn(List.of(c1, c2));

        List<PartLibraryCategoryResponse> result = service.getActiveCategories();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("INTERIOR");
        assertThat(result.get(1).code()).isEqualTo("EXTERIOR");
    }

    @Test
    @DisplayName("Lấy chi tiết danh mục theo ID không tồn tại ném ResourceNotFoundException")
    void getCategoryById_notFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy danh mục kho mẫu & part với ID: 999");
    }

    @Test
    @DisplayName("Lấy danh sách danh mục có phân trang thành công")
    void getCategories_paginated() {
        PartLibraryCategory c1 = new PartLibraryCategory("INTERIOR", "Nội thất", "ACTIVE");
        c1.setId(1L);
        Page<PartLibraryCategory> page = new PageImpl<>(List.of(c1));
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(svgFileRepository.countByPartLibraryCategory_Id(1L)).thenReturn(4L);

        PageResponse<PartLibraryCategoryResponse> result = service.getCategories("ACTIVE", "nội", 0, 10, "name", "asc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).usageCount()).isEqualTo(4L);
    }
}
