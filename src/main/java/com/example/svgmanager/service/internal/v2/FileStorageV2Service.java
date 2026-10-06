package com.example.svgmanager.service.internal.v2;

import java.nio.file.Path;

/**
 * Storage riêng biệt hoàn toàn cho V2.
 * Lưu trữ các file ciphertext (.enc), tuyệt đối không lưu file plaintext .svg.
 */
public interface FileStorageV2Service {

    String storeEncryptedFile(byte[] ciphertext, Long userId, String fileUuid);

    byte[] loadEncryptedFile(String storageKey);

    boolean deleteEncryptedFile(String storageKey);

    Path getRootLocation();
}
