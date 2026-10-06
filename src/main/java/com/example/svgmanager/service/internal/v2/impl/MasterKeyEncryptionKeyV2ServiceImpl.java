package com.example.svgmanager.service.internal.v2.impl;

import com.example.svgmanager.service.internal.v2.EncryptionKeyV2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MasterKeyEncryptionKeyV2ServiceImpl implements EncryptionKeyV2Service {

    private static final Logger log = LoggerFactory.getLogger(MasterKeyEncryptionKeyV2ServiceImpl.class);

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // in bits
    private static final int DEK_SIZE_BYTES = 32;  // 256 bits
    private static final int IV_SIZE_BYTES = 12;   // 96 bits for GCM
    private static final String CURRENT_KEY_VERSION = "v1";

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] masterKeyBytes;

    public MasterKeyEncryptionKeyV2ServiceImpl(
            @Value("${app.encryption.v2.master-key:}") String configuredMasterKey,
            @Value("${jwt.secret:default-secret-seed-v2-fallback-key-32-bytes}") String fallbackSecret
    ) {
        this.masterKeyBytes = resolveMasterKey(configuredMasterKey, fallbackSecret);
        log.info("[V2_KEY_MGMT] Initialized MasterKeyEncryptionKeyV2Service with key version '{}'", CURRENT_KEY_VERSION);
    }

    private byte[] resolveMasterKey(String configured, String fallback) {
        try {
            if (StringUtils.hasText(configured)) {
                String trimmed = configured.trim();
                // Check if base64 encoded
                try {
                    byte[] decoded = Base64.getDecoder().decode(trimmed);
                    if (decoded.length == 32) {
                        return decoded;
                    }
                } catch (IllegalArgumentException ignored) {
                }
                // Otherwise hash to 32 bytes
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                return sha256.digest(trimmed.getBytes(StandardCharsets.UTF_8));
            }

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(fallback.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize V2 master encryption key", e);
        }
    }

    @Override
    public String getCurrentKeyVersion() {
        return CURRENT_KEY_VERSION;
    }

    @Override
    public byte[] generateDek() {
        byte[] dek = new byte[DEK_SIZE_BYTES];
        secureRandom.nextBytes(dek);
        return dek;
    }

    @Override
    public byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    @Override
    public byte[] encryptDek(byte[] dek, String keyVersion) {
        if (dek == null || dek.length == 0) {
            throw new IllegalArgumentException("DEK cannot be null or empty");
        }
        try {
            byte[] dekIv = new byte[IV_SIZE_BYTES];
            secureRandom.nextBytes(dekIv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(masterKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, dekIv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherDek = cipher.doFinal(dek);

            // Output: 12-byte IV + ciphertext (with auth tag)
            ByteBuffer buffer = ByteBuffer.allocate(dekIv.length + cipherDek.length);
            buffer.put(dekIv);
            buffer.put(cipherDek);
            return buffer.array();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt DEK using Master Key", e);
        }
    }

    @Override
    public byte[] decryptDek(byte[] encryptedDekWithIv, String keyVersion) {
        if (encryptedDekWithIv == null || encryptedDekWithIv.length <= IV_SIZE_BYTES) {
            throw new IllegalArgumentException("Invalid encrypted DEK length");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedDekWithIv);
            byte[] dekIv = new byte[IV_SIZE_BYTES];
            buffer.get(dekIv);

            byte[] cipherDek = new byte[buffer.remaining()];
            buffer.get(cipherDek);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(masterKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, dekIv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            return cipher.doFinal(cipherDek);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt DEK using Master Key", e);
        }
    }
}
