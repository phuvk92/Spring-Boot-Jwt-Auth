package com.example.svgmanager.service.internal.v2;

public interface FileEncryptionV2Service {

    EncryptedFileResult encrypt(byte[] plaintextBytes);

    byte[] decrypt(byte[] ciphertext, byte[] encryptedDek, byte[] iv, String algorithm, String keyVersion);
}
