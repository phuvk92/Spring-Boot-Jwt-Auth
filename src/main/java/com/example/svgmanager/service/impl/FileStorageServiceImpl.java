package com.example.svgmanager.service.impl;

import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.FileStorageException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.util.FileUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private final Path rootLocation;

    public FileStorageServiceImpl(@Value("${app.file.upload-dir:./uploads/svg}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Initialized root storage directory at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage directory: " + rootLocation, e);
        }
    }

    @Override
    public String storeFile(byte[] content, String storedFilename) {
        if (content == null || content.length == 0) {
            throw new BadRequestException("Cannot store empty file content");
        }

        FileUtils.validatePathTraversal(storedFilename);

        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        Path directoryPath = rootLocation.resolve(year).resolve(month).normalize();

        try {
            Files.createDirectories(directoryPath);
            Path destinationFile = directoryPath.resolve(storedFilename).normalize();

            if (!destinationFile.startsWith(rootLocation)) {
                throw new BadRequestException("Cannot store file outside configured upload directory");
            }

            Files.write(destinationFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            log.debug("Stored file at {}", destinationFile);

            return year + "/" + month + "/" + storedFilename;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + storedFilename, e);
        }
    }

    @Override
    public Resource loadFileAsResource(String relativePath) {
        FileUtils.validatePathTraversal(relativePath);
        Path filePath = rootLocation.resolve(relativePath).normalize();

        if (!filePath.startsWith(rootLocation)) {
            throw new BadRequestException("Access denied: path traversal attempt detected");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on storage: " + relativePath);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File path malformed: " + relativePath, e);
        }
    }

    @Override
    public InputStream loadFileAsStream(String relativePath) {
        try {
            return loadFileAsResource(relativePath).getInputStream();
        } catch (IOException e) {
            throw new FileStorageException("Could not open stream for file: " + relativePath, e);
        }
    }

    @Override
    public byte[] loadFileAsBytes(String relativePath) {
        try {
            return loadFileAsStream(relativePath).readAllBytes();
        } catch (IOException e) {
            throw new FileStorageException("Could not read bytes for file: " + relativePath, e);
        }
    }

    @Override
    public boolean deleteFile(String relativePath) {
        FileUtils.validatePathTraversal(relativePath);
        Path filePath = rootLocation.resolve(relativePath).normalize();

        if (!filePath.startsWith(rootLocation)) {
            throw new BadRequestException("Access denied: path traversal attempt detected");
        }

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Deleted physical file at: {}", filePath);
            } else {
                log.warn("Physical file not found for deletion: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", filePath, e);
            return false;
        }
    }

    @Override
    public Path getRootLocation() {
        return rootLocation;
    }
}
