package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.BatchSvgUploadResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.SvgMapper;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.impl.SvgServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private FileStorageService fileStorageService;
    @Mock
    private SvgSanitizerService svgSanitizerService;
    @Mock
    private SvgMapper svgMapper;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AuditLogService auditLogService;

    private SvgServiceImpl svgService;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        svgService = new SvgServiceImpl(
                svgFileRepository,
                fileStorageService,
                svgSanitizerService,
                svgMapper,
                currentUserService,
                auditLogService,
                10485760L // 10MB
        );

        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .role(Role.ADMIN)
                .build();

        regularUser = User.builder()
                .id(2L)
                .username("user1")
                .role(Role.USER)
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

        assertThatThrownBy(() -> svgService.batchUploadSvg(files))
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

        assertThatThrownBy(() -> svgService.batchUploadSvg(files))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Admin batch upload succeeds for valid SVG files")
    void batchUploadSvg_ValidFiles_Success() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

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

        BatchSvgUploadResponse result = svgService.batchUploadSvg(files);

        assertThat(result.totalUploaded()).isEqualTo(1);
        assertThat(result.files()).hasSize(1);
        verify(auditLogService, times(1)).log(eq("admin"), eq("ADMIN"), eq("UPLOAD_SVG"), eq("SvgFile"), eq(50L), any());
    }

    @Test
    @DisplayName("Q6: USER tải file được — không còn kiểm quyền đại lý")
    void downloadSvg_User_Success() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgent()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(regularUser);

        SvgFile svgFile = SvgFile.builder()
                .id(50L)
                .originalFilename("test.svg")
                .filePath("/data/svg/uuid.svg")
                .status("ACTIVE")
                .build();
        when(svgFileRepository.findById(50L)).thenReturn(Optional.of(svgFile));
        when(fileStorageService.loadFileAsResource("/data/svg/uuid.svg"))
                .thenReturn(new ByteArrayResource("<svg></svg>".getBytes(StandardCharsets.UTF_8)));

        Resource res = svgService.downloadSvg(50L);
        assertThat(res).isNotNull();
    }

    @Test
    @DisplayName("USER không xem được file đã xoá mềm → 404")
    void downloadSvg_DeletedFile_NotFoundForUser() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgent()).thenReturn(false);
        when(currentUserService.getCurrentUser()).thenReturn(regularUser);

        SvgFile svgFile = SvgFile.builder()
                .id(50L)
                .status("DELETED")
                .build();
        when(svgFileRepository.findById(50L)).thenReturn(Optional.of(svgFile));

        assertThatThrownBy(() -> svgService.downloadSvg(50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("AGENT chỉ xem file mình nạp → file của người khác ra 404")
    void getSvgFileById_AgentNotOwner_ThrowsNotFound() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(
                User.builder().id(7L).role(Role.AGENT).build());
        when(svgFileRepository.findByIdAndAgentId(50L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svgService.getSvgFileById(50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
