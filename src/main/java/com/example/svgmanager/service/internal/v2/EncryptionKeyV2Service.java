package com.example.svgmanager.service.internal.v2;

/**
 * Abstraction quản lý khóa mã hoá cho V2 (Envelope Encryption Key Management).
 * Thiết kế độc lập để sau này có thể cắm AWS KMS, HashiCorp Vault mà không sửa business logic.
 */
public interface EncryptionKeyV2Service {

    String getCurrentKeyVersion();

    byte[] generateDek();

    byte[] generateIv();

    byte[] encryptDek(byte[] dek, String keyVersion);

    byte[] decryptDek(byte[] encryptedDek, String keyVersion);
}
