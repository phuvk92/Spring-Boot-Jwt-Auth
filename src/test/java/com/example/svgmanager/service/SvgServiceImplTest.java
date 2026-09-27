package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.SvgFileDealerPermissionRequest;
import com.example.svgmanager.dto.response.BatchSvgUploadResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.SvgMapper;
import com.example.svgmanager.mapper.VehicleConfigurationMapper;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.impl.SvgServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SvgServiceImplTest {

    @Mock
    private SvgFileRepository svgFileRepository;
    @Mock
    private SvgFileVehicleConfigurationRepository svgVehicleConfigRepository;
    @Mock
    private SvgFileDealerPermissionRepository svgDealerPermissionRepository;
    @Mock
    private VehicleConfigurationRepository vehicleConfigurationRepository;
    @Mock
    private DealerRepository dealerRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private SvgSanitizerService svgSanitizerService;
    @Mock
    private SvgMapper svgMapper;
    @Mock
    private VehicleConfigurationMapper vehicleConfigurationMapper;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AuditLogService auditLogService;

    private SvgServiceImpl svgService;

    private User adminUser;
    private User regularUser;
    private Dealer dealerA;

    @BeforeEach
    void setUp() {
        svgService = new SvgServiceImpl(
                svgFileRepository,
                svgVehicleConfigRepository,
                svgDealerPermissionRepository,
                vehicleConfigurationRepository,
                dealerRepository,
                fileStorageService,
                svgSanitizerService,
                svgMapper,
                vehicleConfigurationMapper,
                currentUserService,
                categoryService,
                auditLogService,
                10485760L // 10MB
        );

        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .role(Role.ADMIN)
                .build();

        dealerA = Dealer.builder()
                .id(10L)
                .code("DL001")
                .name("Dealer Alpha")
                .build();

        regularUser = User.builder()
                .id(2L)
                .username("user1")
                .role(Role.USER)
                .dealer(dealerA)
                .build();
    }

    @Test
    @DisplayName("Admin upload rejects request if files exceed max 10 files")
    void batchUploadSvg_ExceedMaxFiles_ThrowsBadRequest() {
        when(currentUserService.isAdmin()).thenReturn(true);

        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            files.add(new MockMultipartFile("files", "file" + i + ".svg", "image/svg+xml", "<svg></svg>".getBytes(StandardCharsets.UTF_8)));
        }

        assertThatThrownBy(() -> svgService.batchUploadSvg(files, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tối đa 10 file");
    }

    @Test
    @DisplayName("Non-admin cannot batch upload files")
    void batchUploadSvg_NonAdmin_ThrowsForbidden() {
        when(currentUserService.isAdmin()).thenReturn(false);

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "test.svg", "image/svg+xml", "<svg></svg>".getBytes(StandardCharsets.UTF_8))
        );

        assertThatThrownBy(() -> svgService.batchUploadSvg(files, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Admin batch upload succeeds for valid SVG files with configuration and dealer permission")
    void batchUploadSvg_ValidFiles_Success() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        VehicleConfiguration config = VehicleConfiguration.builder().id(100L).productGroup(ProductGroup.PPF_EXTERIOR).build();
        when(vehicleConfigurationRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(config));
        when(dealerRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(dealerA));

        byte[] svgContent = "<svg><rect width=\"10\" height=\"10\"/></svg>".getBytes(StandardCharsets.UTF_8);
        when(svgSanitizerService.sanitizeAndValidateSvg(any())).thenReturn(svgContent);
        when(fileStorageService.storeFile(any(), any())).thenReturn("/data/svg/uuid.svg");

        SvgFile savedFile = SvgFile.builder()
                .id(50L)
                .originalFilename("test.svg")
                .fileSize((long) svgContent.length)
                .build();
        when(svgFileRepository.save(any(SvgFile.class))).thenReturn(savedFile);

        SvgResponse response = SvgResponse.builder().id(50L).originalFilename("test.svg").build();
        when(svgMapper.toSvgResponse(eq(savedFile), eq(adminUser), eq(true))).thenReturn(response);

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "test.svg", "image/svg+xml", svgContent)
        );

        List<SvgFileDealerPermissionRequest> dealerPerms = List.of(
                new SvgFileDealerPermissionRequest(10L, true, true)
        );

        BatchSvgUploadResponse result = svgService.batchUploadSvg(files, List.of(100L), dealerPerms);

        assertThat(result.totalUploaded()).isEqualTo(1);
        assertThat(result.files()).hasSize(1);
        verify(svgVehicleConfigRepository, times(1)).save(any(SvgFileVehicleConfiguration.class));
        verify(svgDealerPermissionRepository, times(1)).save(any(SvgFileDealerPermission.class));
        verify(auditLogService, times(1)).log(eq("admin"), eq("ADMIN"), eq("UPLOAD_SVG"), eq("SvgFile"), eq(50L), any());
    }

    @Test
    @DisplayName("User without dealer receives empty page response")
    void getSvgFiles_UserWithoutDealer_ReturnsEmpty() {
        User userWithoutDealer = User.builder().id(3L).role(Role.USER).dealer(null).build();
        when(currentUserService.getCurrentUser()).thenReturn(userWithoutDealer);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgent()).thenReturn(false);

        PageResponse<SvgResponse> result = svgService.getSvgFiles(null, null, null, null, null, null, null, null, 0, 10, "createdAt", "DESC");
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("User download fails with 403 Forbidden when dealer has canDownload = false")
    void downloadSvg_DealerNoDownload_ThrowsForbidden() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(regularUser);

        SvgFileDealerPermission perm = new SvgFileDealerPermission(null, dealerA, true, false);
        when(svgDealerPermissionRepository.findBySvgFileIdAndDealerId(50L, 10L)).thenReturn(Optional.of(perm));

        assertThatThrownBy(() -> svgService.downloadSvg(50L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("DOWNLOAD_PERMISSION_DENIED");
    }

    @Test
    @DisplayName("User download succeeds when dealer has canDownload = true")
    void downloadSvg_DealerCanDownload_Success() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(regularUser);

        SvgFileDealerPermission perm = new SvgFileDealerPermission(null, dealerA, true, true);
        when(svgDealerPermissionRepository.findBySvgFileIdAndDealerId(50L, 10L)).thenReturn(Optional.of(perm));

        SvgFile svgFile = SvgFile.builder()
                .id(50L)
                .originalFilename("test.svg")
                .filePath("/data/svg/uuid.svg")
                .build();
        when(svgFileRepository.findById(50L)).thenReturn(Optional.of(svgFile));
        when(fileStorageService.loadFileAsResource("/data/svg/uuid.svg"))
                .thenReturn(new ByteArrayResource("<svg></svg>".getBytes(StandardCharsets.UTF_8)));

        Resource res = svgService.downloadSvg(50L);
        assertThat(res).isNotNull();
    }

    @Test
    @DisplayName("Updating dealer permission validates canDownload requires canView")
    void updateDealerPermissions_CanDownloadWithoutCanView_ThrowsBadRequest() {
        when(currentUserService.isAdmin()).thenReturn(true);
        SvgFile svgFile = SvgFile.builder().id(50L).build();
        when(svgFileRepository.findById(50L)).thenReturn(Optional.of(svgFile));

        List<SvgFileDealerPermissionRequest> invalidPerms = List.of(
                new SvgFileDealerPermissionRequest(10L, false, true) // canView=false, canDownload=true
        );

        assertThatThrownBy(() -> svgService.updateDealerPermissions(50L, invalidPerms))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể cấp quyền tải xuống khi không có quyền xem");
    }
}
