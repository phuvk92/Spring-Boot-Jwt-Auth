package com.example.svgmanager.service;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorageService {

    String storeFile(byte[] fileBytes, String storedFilename);

    Resource loadFileAsResource(String filePath);

    InputStream loadFileAsStream(String filePath);

    byte[] loadFileAsBytes(String filePath);

    boolean deleteFile(String filePath);

    Path getRootLocation();
}
