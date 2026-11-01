package com.example.svgmanager.service.internal.v2.impl;

import com.example.svgmanager.entity.v2.ClientSvgShare;
import com.example.svgmanager.service.internal.v2.ClientShareTokenService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class ClientShareTokenServiceImpl implements ClientShareTokenService {

    private static final Logger log = LoggerFactory.getLogger(ClientShareTokenServiceImpl.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secretKeyBytes;
    private final long defaultValiditySeconds;
    private final ObjectMapper objectMapper;

    public ClientShareTokenServiceImpl(
            @Value("${app.share.token-secret:cutting-internal-share-token-secret-key-v2-at-least-32-chars!}") String secret,
            @Value("${app.share.token-expiration-seconds:3600}") long defaultValiditySeconds,
            ObjectMapper objectMapper
    ) {
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.defaultValiditySeconds = defaultValiditySeconds > 0 ? defaultValiditySeconds : 3600;
        this.objectMapper = objectMapper;
    }

    public static class ShareTokenPayload {
        @JsonProperty("sub")
        private String shareId;

        @JsonProperty("ver")
        private int tokenVersion;

        @JsonProperty("iat")
        private long issuedAt;

        @JsonProperty("exp")
        private long expiresAt;

        public ShareTokenPayload() {
        }

        public ShareTokenPayload(String shareId, int tokenVersion, long issuedAt, long expiresAt) {
            this.shareId = shareId;
            this.tokenVersion = tokenVersion;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }

        public String getShareId() {
            return shareId;
        }

        public void setShareId(String shareId) {
            this.shareId = shareId;
        }

        public int getTokenVersion() {
            return tokenVersion;
        }

        public void setTokenVersion(int tokenVersion) {
            this.tokenVersion = tokenVersion;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public void setIssuedAt(long issuedAt) {
            this.issuedAt = issuedAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    @Override
    public String generateToken(ClientSvgShare share, long validitySeconds) {
        long effectiveValidity = validitySeconds > 0 ? validitySeconds : defaultValiditySeconds;
        long now = Instant.now().getEpochSecond();
        long exp = now + effectiveValidity;

        // If the share itself expires earlier than token, cap the expiration
        if (share.getExpiresAt() != null) {
            long shareExpEpoch = share.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            if (shareExpEpoch < exp) {
                exp = shareExpEpoch;
            }
        }

        ShareTokenPayload payload = new ShareTokenPayload(
                share.getShareId(),
                share.getTokenVersion(),
                now,
                exp
        );

        try {
            byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);
            byte[] signatureBytes = sign(encodedPayload.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            return encodedPayload + "." + encodedSignature;
        } catch (Exception e) {
            log.error("[SHARE_TOKEN_ERROR] Failed to generate token for shareId={}: {}", share.getShareId(), e.getMessage());
            throw new IllegalStateException("Không thể tạo token chia sẻ", e);
        }
    }

    @Override
    public boolean validateToken(String token, ClientSvgShare share) {
        if (token == null || token.isBlank() || share == null) {
            return false;
        }

        // Strict immediate revocation check against database
        if (share.isRevoked() || !share.isActive()) {
            log.warn("[SHARE_TOKEN_REJECTED] Share is revoked or inactive. shareId={}", share.getShareId());
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return false;
        }

        try {
            String encodedPayload = parts[0];
            String encodedSignature = parts[1];

            byte[] expectedSignature = sign(encodedPayload.getBytes(StandardCharsets.UTF_8));
            byte[] actualSignature = Base64.getUrlDecoder().decode(encodedSignature);

            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                log.warn("[SHARE_TOKEN_REJECTED] Invalid signature for shareId={}", share.getShareId());
                return false;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(encodedPayload);
            ShareTokenPayload payload = objectMapper.readValue(payloadBytes, ShareTokenPayload.class);

            long now = Instant.now().getEpochSecond();
            if (payload.getExpiresAt() < now) {
                log.warn("[SHARE_TOKEN_REJECTED] Token expired for shareId={}", share.getShareId());
                return false;
            }

            if (!share.getShareId().equals(payload.getShareId())) {
                log.warn("[SHARE_TOKEN_REJECTED] Mismatched shareId in token: expected={}, actual={}",
                        share.getShareId(), payload.getShareId());
                return false;
            }

            if (payload.getTokenVersion() != share.getTokenVersion()) {
                log.warn("[SHARE_TOKEN_REJECTED] Token version revoked for shareId={}: expected={}, actual={}",
                        share.getShareId(), share.getTokenVersion(), payload.getTokenVersion());
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("[SHARE_TOKEN_REJECTED] Token parsing error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public long getDefaultTokenValiditySeconds() {
        return defaultValiditySeconds;
    }

    private byte[] sign(byte[] data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM));
        return mac.doFinal(data);
    }
}
