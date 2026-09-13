package com.example.svgmanager.util;

import com.example.svgmanager.exception.BadRequestException;
import org.springframework.util.StringUtils;

import java.nio.file.Paths;

public final class FileUtils {

    private FileUtils() {
    }

    public static String getCleanFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Filename cannot be empty");
        }

        // Clean path and extract only the filename part
        String cleanPath = StringUtils.cleanPath(originalFilename);
        String filename = Paths.get(cleanPath).getFileName().toString();

        // Check for path traversal characters
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.contains("\0")) {
            throw new BadRequestException("Filename contains invalid path traversal characters");
        }

        return filename;
    }

    public static void validatePathTraversal(String path) {
        if (path == null || path.isBlank()) {
            throw new BadRequestException("Path cannot be empty");
        }
        if (path.contains("..") || path.contains("\0")) {
            throw new BadRequestException("Path contains invalid traversal sequences");
        }
    }

    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public static boolean isSvgExtension(String filename) {
        return "svg".equalsIgnoreCase(getFileExtension(filename));
    }
}
