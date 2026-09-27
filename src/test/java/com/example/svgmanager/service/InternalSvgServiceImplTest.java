package com.example.svgmanager.service;

import com.example.svgmanager.dto.internal.InternalSvgDetailResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.SvgFileDealerPermissionRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.SvgFileVehicleConfigurationRepository;
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

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InternalSvgServiceImplTest {

    @Mock
    private SvgFileRepository svgFileRepository;

    @Mock
    private SvgFileDealerPermissionRepository dealerPermissionRepository;

    @Mock
    private SvgFileVehicleConfigurationRepository vehicleConfigRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InternalSvgServiceImpl internalSvgService;

    private Dealer dealerA;
    private Dealer dealerB;
    private User adminUser;
    private User userInDealerA;
    private User userInDealerB;
    private User userNoDealer;
    private SvgFile sampleSvg;
    private VehicleConfiguration vehicleConfig;

    @BeforeEach
    void setUp() {
        dealerA = Dealer.builder().id(10L).code("DLA").name("Dealer A").status("ACTIVE").build();
        dealerB = Dealer.builder().id(20L).code("DLB").name("Dealer B").status("ACTIVE").build();

        adminUser = User.builder().id(1L).username("admin").role(Role.ADMIN).enabled(true).build();
        userInDealerA = User.builder().id(2L).username("userA").role(Role.USER).dealer(dealerA).enabled(true).build();
        userInDealerB = User.builder().id(3L).username("userB").role(Role.USER).dealer(dealerB).enabled(true).build();
        userNoDealer = User.builder().id(4L).username("userNoDealer").role(Role.USER).dealer(null).enabled(true).build();

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

        CarBrand brand = CarBrand.builder().id(1L).name("Toyota").code("TOYOTA").build();
        CarModel model = CarModel.builder().id(1L).brand(brand).name("Camry").code("CAMRY").build();
        vehicleConfig = VehicleConfiguration.builder()
                .id(50L)
                .productGroup(ProductGroup.PPF_EXTERIOR)
                .brand(brand)
                .model(model)
                .yearFrom(2022)
                .yearTo(2025)
                .generationCode("XV70")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Admin can view SVG detail regardless of dealer permissions")
    void testGetSvgDetail_Admin_Success() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        SvgFileVehicleConfiguration svc = new SvgFileVehicleConfiguration(sampleSvg, vehicleConfig);
        when(vehicleConfigRepository.findBySvgFileId(100L)).thenReturn(List.of(svc));

        InternalSvgDetailResponse response = internalSvgService.getSvgDetail(100L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.fileName()).isEqualTo("test_pattern.svg");
        assertThat(response.permission().canView()).isTrue();
        assertThat(response.permission().canDownload()).isTrue();
        assertThat(response.vehicleConfigurations()).hasSize(1);
        assertThat(response.vehicleConfigurations().get(0).brandName()).isEqualTo("Toyota");
        assertThat(response.vehicleConfigurations().get(0).modelName()).isEqualTo("Camry");

        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("SVG_VIEW"), eq("SvgFile"), eq(100L), any());
    }

    @Test
    @DisplayName("User with canView=true and canDownload=true can view SVG detail")
    void testGetSvgDetail_UserWithPermissions_Success() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(userInDealerA);

        SvgFileDealerPermission permission = new SvgFileDealerPermission(sampleSvg, dealerA, true, true);
        when(dealerPermissionRepository.findBySvgFileIdAndDealerId(100L, 10L)).thenReturn(Optional.of(permission));

        InternalSvgDetailResponse response = internalSvgService.getSvgDetail(100L);

        assertThat(response).isNotNull();
        assertThat(response.permission().canView()).isTrue();
        assertThat(response.permission().canDownload()).isTrue();
    }

    @Test
    @DisplayName("User without dealer receives 404 when viewing SVG")
    void testGetSvgDetail_UserNoDealer_ThrowsNotFound() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(userNoDealer);

        assertThatThrownBy(() -> internalSvgService.getSvgDetail(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SVG file not found with id: 100");
    }

    @Test
    @DisplayName("User with canView=false receives 404 to avoid leaking resource existence")
    void testGetSvgDetail_UserNoPermission_ThrowsNotFound() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(userInDealerB);

        SvgFileDealerPermission permission = new SvgFileDealerPermission(sampleSvg, dealerB, false, false);
        when(dealerPermissionRepository.findBySvgFileIdAndDealerId(100L, 20L)).thenReturn(Optional.of(permission));

        assertThatThrownBy(() -> internalSvgService.getSvgDetail(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SVG file not found with id: 100");
    }

    @Test
    @DisplayName("User whose dealer is not assigned receives 404")
    void testGetSvgDetail_DealerNotAssigned_ThrowsNotFound() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(userInDealerB);

        when(dealerPermissionRepository.findBySvgFileIdAndDealerId(100L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalSvgService.getSvgDetail(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SVG file not found with id: 100");
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
        when(currentUserService.isAdmin()).thenReturn(true);
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
    @DisplayName("User with canDownload=true can download SVG file")
    void testDownloadSvg_UserCanDownload_Success() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(userInDealerA);

        SvgFileDealerPermission permission = new SvgFileDealerPermission(sampleSvg, dealerA, true, true);
        when(dealerPermissionRepository.findBySvgFileIdAndDealerId(100L, 10L)).thenReturn(Optional.of(permission));

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
    @DisplayName("User with canView=true but canDownload=false receives 403 Forbidden")
    void testDownloadSvg_UserViewOnly_ThrowsForbidden() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(userInDealerA);

        SvgFileDealerPermission permission = new SvgFileDealerPermission(sampleSvg, dealerA, true, false);
        when(dealerPermissionRepository.findBySvgFileIdAndDealerId(100L, 10L)).thenReturn(Optional.of(permission));

        assertThatThrownBy(() -> internalSvgService.downloadSvg(100L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not have permission to download this file");
    }

    @Test
    @DisplayName("User with canView=false receives 404 on download attempt")
    void testDownloadSvg_UserNoView_ThrowsNotFound() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(userInDealerB);

        SvgFileDealerPermission permission = new SvgFileDealerPermission(sampleSvg, dealerB, false, false);
        when(dealerPermissionRepository.findBySvgFileIdAndDealerId(100L, 20L)).thenReturn(Optional.of(permission));

        assertThatThrownBy(() -> internalSvgService.downloadSvg(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Physical file missing on storage throws 404")
    void testDownloadSvg_StorageFileMissing_ThrowsNotFound() {
        when(svgFileRepository.findById(100L)).thenReturn(Optional.of(sampleSvg));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        when(fileStorageService.loadFileAsResource(sampleSvg.getFilePath())).thenReturn(null);

        assertThatThrownBy(() -> internalSvgService.downloadSvg(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found on storage");
    }
}
