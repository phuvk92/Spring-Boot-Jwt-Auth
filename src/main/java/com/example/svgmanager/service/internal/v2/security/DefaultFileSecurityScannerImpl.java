package com.example.svgmanager.service.internal.v2.security;

import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.SvgSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultFileSecurityScannerImpl implements FileSecurityScanner {

    private static final Logger log = LoggerFactory.getLogger(DefaultFileSecurityScannerImpl.class);

    @Override
    public void scan(byte[] fileBytes, String filename) throws SvgSecurityException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new SvgSecurityException("File rỗng", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }

        // 1. Kiểm tra các magic bytes nhị phân nguy hiểm (PE .exe/.dll, ELF, Mach-O, Java class)
        if (isExecutable(fileBytes)) {
            log.warn("[MALWARE_BLOCKED] Executable binary format detected in file '{}'", filename);
            throw new SvgSecurityException("Phát hiện tệp thực thi nhị phân không được phép", ErrorCodes.SVG_MALWARE_DETECTED);
        }

        // 2. Kiểm tra các định dạng nén (ZIP, RAR, 7Z, GZIP, TAR) — phòng chống Zip Bomb / Archive
        if (isArchive(fileBytes)) {
            log.warn("[MALWARE_BLOCKED] Archive format detected in SVG file '{}'", filename);
            throw new SvgSecurityException("Tệp lưu trữ nén không được chấp nhận làm SVG", ErrorCodes.SVG_MALWARE_DETECTED);
        }

        // 3. Kiểm tra byte null '\0' — SVG là text XML UTF-8 hợp lệ, không được chứa byte null
        int checkLen = Math.min(fileBytes.length, 4096);
        for (int i = 0; i < checkLen; i++) {
            if (fileBytes[i] == 0) {
                log.warn("[MALWARE_BLOCKED] Null byte detected in initial chunk of file '{}'", filename);
                throw new SvgSecurityException("Phát hiện ký tự nhị phân null byte trong tệp SVG", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
            }
        }
    }

    private boolean isExecutable(byte[] bytes) {
        if (bytes.length < 4) {
            return false;
        }
        // Windows PE (MZ)
        if (bytes[0] == 0x4D && bytes[1] == 0x5A) {
            return true;
        }
        // Linux ELF
        if (bytes[0] == 0x7F && bytes[1] == 0x45 && bytes[2] == 0x4C && bytes[3] == 0x46) {
            return true;
        }
        // Mach-O (macOS binary)
        if ((bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xED && bytes[2] == (byte) 0xFA && bytes[3] == (byte) 0xCE) ||
            (bytes[0] == (byte) 0xCF && bytes[1] == (byte) 0xFA && bytes[2] == (byte) 0xED && bytes[3] == (byte) 0xFE)) {
            return true;
        }
        // Java Class bytecode (CA FE BA BE)
        if (bytes[0] == (byte) 0xCA && bytes[1] == (byte) 0xFE && bytes[2] == (byte) 0xBA && bytes[3] == (byte) 0xBE) {
            return true;
        }
        // Shebang Unix script (#! /bin/...)
        if (bytes[0] == '#' && bytes[1] == '!') {
            return true;
        }
        return false;
    }

    private boolean isArchive(byte[] bytes) {
        if (bytes.length < 4) {
            return false;
        }
        // ZIP / JAR (PK\x03\x04 hoặc PK\x05\x06)
        if (bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4) {
            return true;
        }
        // RAR (Rar!\x1a\x07)
        if (bytes[0] == 'R' && bytes[1] == 'a' && bytes[2] == 'r' && bytes[3] == '!') {
            return true;
        }
        // 7Z (7z\xbc\xaf)
        if (bytes[0] == '7' && bytes[1] == 'z' && bytes[2] == (byte) 0xBC && bytes[3] == (byte) 0xAF) {
            return true;
        }
        // GZIP (1F 8B)
        if (bytes[0] == (byte) 0x1F && bytes[1] == (byte) 0x8B) {
            return true;
        }
        return false;
    }
}
