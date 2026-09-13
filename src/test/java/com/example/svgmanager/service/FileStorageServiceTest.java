package com.example.svgmanager.service;

import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempUploadDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl(tempUploadDir.toString());
        ((FileStorageServiceImpl) fileStorageService).init();
    }

    @Test
    @DisplayName("Should successfully store file in YYYY/MM/uuid.svg structure")
    void storeFile_Success() {
        byte[] content = "<svg><circle r='10'/></svg>".getBytes(StandardCharsets.UTF_8);
        String filename = "test-uuid-123.svg";

        String relativePath = fileStorageService.storeFile(content, filename);

        LocalDate now = LocalDate.now();
        String expectedPrefix = now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/";

        assertThat(relativePath).startsWith(expectedPrefix);
        assertThat(relativePath).endsWith("test-uuid-123.svg");

        Path physicalPath = tempUploadDir.resolve(relativePath);
        assertThat(Files.exists(physicalPath)).isTrue();
    }

    @Test
    @DisplayName("Should reject storing empty content")
    void storeFile_EmptyContent() {
        assertThatThrownBy(() -> fileStorageService.storeFile(new byte[0], "empty.svg"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot store empty file");
    }

    @Test
    @DisplayName("Should reject path traversal attempts during file storage")
    void storeFile_PathTraversal() {
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> fileStorageService.storeFile(content, "../../evil.svg"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should load stored file as Spring Resource")
    void loadFileAsResource_Success() {
        byte[] content = "<svg><rect width='10' height='10'/></svg>".getBytes(StandardCharsets.UTF_8);
        String relativePath = fileStorageService.storeFile(content, "sample.svg");

        Resource resource = fileStorageService.loadFileAsResource(relativePath);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent file")
    void loadFileAsResource_NotFound() {
        assertThatThrownBy(() -> fileStorageService.loadFileAsResource("2026/01/non-existent.svg"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should successfully delete stored physical file")
    void deleteFile_Success() {
        byte[] content = "<svg><path d='M0 0h10v10H0z'/></svg>".getBytes(StandardCharsets.UTF_8);
        String relativePath = fileStorageService.storeFile(content, "delete-me.svg");

        Path physicalPath = tempUploadDir.resolve(relativePath);
        assertThat(Files.exists(physicalPath)).isTrue();

        boolean deleted = fileStorageService.deleteFile(relativePath);

        assertThat(deleted).isTrue();
        assertThat(Files.exists(physicalPath)).isFalse();
    }
}
