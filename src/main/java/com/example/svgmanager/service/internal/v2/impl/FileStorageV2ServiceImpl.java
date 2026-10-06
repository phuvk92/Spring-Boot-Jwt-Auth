package com.example.svgmanager.service.internal.v2.impl;

import com.example.svgmanager.exception.FileStorageException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
import com.example.svgmanager.util.FileUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class FileStorageV2ServiceImpl implements FileStorageV2Service {

    private static final Logger log = LoggerFactory.getLogger(FileStorageV2ServiceImpl.class);

    private final Path rootLocation;

    public FileStorageV2ServiceImpl(
            @Value("${app.file.v2.storage-path:}") String v2Path,
            @Value("${app.file.storage-path:${app.file.upload-dir:./uploads/svg}}") String basePath
    ) {
        String effectivePath = org.springframework.util.StringUtils.hasText(v2Path)
                ? v2Path
                : basePath + "/v2/user-files";
        this.rootLocation = Paths.get(effectivePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("[V2_STORAGE_INIT] Initialized V2 encrypted storage directory at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize V2 storage directory: " + rootLocation, e);
        }
    }

    @Override
    public String storeEncryptedFile(byte[] ciphertext, Long userId, String fileUuid) {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Dữ liệu ciphertext không được để trống");
        }

        String userFolder = userId != null ? String.valueOf(userId) : "common";
        String encFilename = (fileUuid != null ? fileUuid : java.util.UUID.randomUUID().toString()) + ".enc";

        FileUtils.validatePathTraversal(encFilename);
        FileUtils.validatePathTraversal(userFolder);

        Path userDir = rootLocation.resolve(userFolder).normalize();
        try {
            Files.createDirectories(userDir);
            Path destinationFile = userDir.resolve(encFilename).normalize();

            if (!destinationFile.startsWith(rootLocation)) {
                throw new FileStorageException("Security violation: path traversal outside V2 storage root");
            }

            Files.write(destinationFile, ciphertext,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

            // Storage key: v2/user-files/{userId}/{uuid}.enc
            String storageKey = "v2/user-files/" + userFolder + "/" + encFilename;
            log.info("[V2_STORAGE_SAVED] Stored encrypted file {} ({} bytes)", storageKey, ciphertext.length);
            return storageKey;
        } catch (IOException e) {
            log.error("[V2_STORAGE_ERROR] Không thể lưu file mã hoá: {}", encFilename, e);
            throw new FileStorageException("Failed to store encrypted file in V2 storage: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] loadEncryptedFile(String storageKey) {
        Path filePath = resolveKeyToPath(storageKey);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new ResourceNotFoundException("File mã hoá V2 không tồn tại trên ổ đĩa");
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read encrypted file from V2 storage: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteEncryptedFile(String storageKey) {
        Path filePath = resolveKeyToPath(storageKey);
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("[V2_STORAGE_DELETE_WARN] Không thể xoá file vật lý V2: {}", storageKey, e);
            return false;
        }
    }

    @Override
    public Path getRootLocation() {
        return rootLocation;
    }

    private Path resolveKeyToPath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey cannot be blank");
        }
        String relative = storageKey;
        if (relative.startsWith("v2/user-files/")) {
            relative = relative.substring("v2/user-files/".length());
        }
        Path resolved = rootLocation.resolve(relative).normalize();
        if (!resolved.startsWith(rootLocation)) {
            throw new FileStorageException("Security violation: path traversal detected");
        }
        return resolved;
    }
}
