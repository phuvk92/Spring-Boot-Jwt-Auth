package com.example.svgmanager.service.internal.v2;

public record EncryptedFileResult(
        byte[] ciphertext,
        byte[] encryptedDek,
        byte[] iv,
        String algorithm,
        String keyVersion,
        String plaintextChecksum,
        String encryptedChecksum
) {
}
