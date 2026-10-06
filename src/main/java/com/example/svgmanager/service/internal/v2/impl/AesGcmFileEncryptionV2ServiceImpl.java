package com.example.svgmanager.service.internal.v2.impl;

import com.example.svgmanager.service.internal.v2.EncryptedFileResult;
import com.example.svgmanager.service.internal.v2.EncryptionKeyV2Service;
import com.example.svgmanager.service.internal.v2.FileEncryptionV2Service;
import com.example.svgmanager.util.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

@Service
public class AesGcmFileEncryptionV2ServiceImpl implements FileEncryptionV2Service {

    private static final Logger log = LoggerFactory.getLogger(AesGcmFileEncryptionV2ServiceImpl.class);

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // in bits

    private final EncryptionKeyV2Service encryptionKeyV2Service;

    public AesGcmFileEncryptionV2ServiceImpl(EncryptionKeyV2Service encryptionKeyV2Service) {
        this.encryptionKeyV2Service = encryptionKeyV2Service;
    }

    @Override
    public EncryptedFileResult encrypt(byte[] plaintextBytes) {
        if (plaintextBytes == null || plaintextBytes.length == 0) {
            throw new IllegalArgumentException("Nội dung file không được để trống khi mã hoá");
        }

        String plaintextChecksum = "sha256:" + ChecksumUtils.calculateSha256(plaintextBytes);
        String keyVersion = encryptionKeyV2Service.getCurrentKeyVersion();

        byte[] dek = encryptionKeyV2Service.generateDek();
        byte[] iv = encryptionKeyV2Service.generateIv();

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(dek, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintextBytes);
            String encryptedChecksum = "sha256:" + ChecksumUtils.calculateSha256(ciphertext);

            byte[] encryptedDek = encryptionKeyV2Service.encryptDek(dek, keyVersion);

            return new EncryptedFileResult(
                    ciphertext,
                    encryptedDek,
                    iv,
                    "AES-256-GCM",
                    keyVersion,
                    plaintextChecksum,
                    encryptedChecksum
            );
        } catch (Exception e) {
            log.error("[V2_ENCRYPTION_FAILED] Lỗi mã hoá AES-256-GCM", e);
            throw new IllegalStateException("Không thể mã hoá file SVG V2: " + e.getMessage(), e);
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] encryptedDek, byte[] iv, String algorithm, String keyVersion) {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Dữ liệu mã hoá không được để trống");
        }
        if (encryptedDek == null || iv == null) {
            throw new IllegalArgumentException("Metadata mã hoá (DEK/IV) không hợp lệ");
        }

        byte[] dek = encryptionKeyV2Service.decryptDek(encryptedDek, keyVersion);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(dek, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            log.error("[V2_DECRYPTION_FAILED] Lỗi giải mã AES-256-GCM", e);
            throw new IllegalStateException("Không thể giải mã file SVG V2: " + e.getMessage(), e);
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }
}
