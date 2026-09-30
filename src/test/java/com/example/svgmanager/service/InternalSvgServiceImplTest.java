package com.example.svgmanager.service;

import com.example.svgmanager.dto.internal.InternalSvgDetailResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.SvgFileVehicleNodeRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.impl.InternalSvgServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Sau V15/Q6: không còn quyền đại lý — phiên hợp lệ xem/tải được mọi file còn hiệu lực.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InternalSvgServiceImplTest {

    @Mock
    private SvgFileRepository svgFileRepository;

    @Mock
    private SvgFileVehicleNodeRepository vehicleNodeLinkRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InternalSvgServiceImpl internalSvgService;

    private User adminUser;
    private User workerUser;
    private SvgFile sampleSvg;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id(1L).username("admin").role(Role.ADMIN).enabled(true).build();
        workerUser = User.builder().id(2L).username("worker").role(Role.USER).enabled(true).build();

        sampleSvg = SvgFile.builder()
                .id(100L)
                .originalFilename("test_pattern.svg")
                .storedFilename("stored_uuid.svg")
                .filePath("/storage/stored_uuid.svg")
                .fileSize(1024L)
                .contentType("image/svg+xml")
                .checksum("sha256:abc123456")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Admin xem chi tiết — kèm đường dẫn xe của các node đã gắn")
    void testGetSvgDetail_Admin_Success() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        VehicleNode brand = new VehicleNode(null, VehicleNodeLevel.BRAND, "Toyota", 1);
        VehicleNode series = new VehicleNode(brand, VehicleNodeLevel.SERIES, "Camry", 1);
        when(vehicleNodeLinkRepository.findBySvgFileId(100L))
                .thenReturn(List.of(new SvgFileVehicleNode(sampleSvg, series)));

        InternalSvgDetailResponse response = internalSvgService.getSvgDetail(100L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.fileName()).isEqualTo("test_pattern.svg");
        assertThat(response.permission().canView()).isTrue();
        assertThat(response.permission().canDownload()).isTrue();
        assertThat(response.vehicles()).containsExactly("Toyota › Camry");

        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("SVG_VIEW"), eq("SvgFile"), eq(100L), any());
    }

    @Test
    @DisplayName("Q6: USER xem chi tiết được mà không cần đại lý/quyền")
    void testGetSvgDetail_User_Success() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.getCurrentUser()).thenReturn(workerUser);
        when(vehicleNodeLinkRepository.findBySvgFileId(100L)).thenReturn(List.of());

        InternalSvgDetailResponse response = internalSvgService.getSvgDetail(100L);

        assertThat(response.permission().canView()).isTrue();
        assertThat(response.permission().canDownload()).isTrue();
        assertThat(response.vehicles()).isEmpty();
    }

    @Test
    @DisplayName("Non-existent SVG ID throws 404")
    void testGetSvgDetail_NonExistentSvg_ThrowsNotFound() {
        when(svgFileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalSvgService.getSvgDetail(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SVG file not found with id: 999");
    }

    @Test
    @DisplayName("Deleted SVG throws 404")
    void testGetSvgDetail_DeletedSvg_ThrowsNotFound() {
        sampleSvg.setStatus("DELETED");
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));

        assertThatThrownBy(() -> internalSvgService.getSvgDetail(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SVG file not found with id: 100");
    }

    @Test
    @DisplayName("Admin can download SVG file")
    void testDownloadSvg_Admin_Success() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        Resource mockResource = new ByteArrayResource("<svg></svg>".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public boolean exists() {
                return true;
            }
            @Override
            public boolean isReadable() {
                return true;
            }
        };
        when(fileStorageService.loadFileAsResource(sampleSvg.getFilePath())).thenReturn(mockResource);

        Resource result = internalSvgService.downloadSvg(100L);

        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("SVG_DOWNLOAD"), eq("SvgFile"), eq(100L), any());
    }

    @Test
    @DisplayName("Q6: USER tải file được — chỉ còn kiểm phiên")
    void testDownloadSvg_User_Success() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.getCurrentUser()).thenReturn(workerUser);

        Resource mockResource = new ByteArrayResource("<svg></svg>".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public boolean exists() {
                return true;
            }
            @Override
            public boolean isReadable() {
                return true;
            }
        };
        when(fileStorageService.loadFileAsResource(sampleSvg.getFilePath())).thenReturn(mockResource);

        Resource result = internalSvgService.downloadSvg(100L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Physical file missing on storage throws 404")
    void testDownloadSvg_StorageFileMissing_ThrowsNotFound() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));

        when(fileStorageService.loadFileAsResource(sampleSvg.getFilePath())).thenReturn(null);

        assertThatThrownBy(() -> internalSvgService.downloadSvg(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found on storage");
    }
}
