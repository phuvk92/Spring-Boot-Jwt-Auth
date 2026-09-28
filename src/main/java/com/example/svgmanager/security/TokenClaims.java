package com.example.svgmanager.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Đọc claim từ token Keycloak mà KHÔNG kiểm chữ ký — chỉ dùng cho token backend vừa nhận
 * trực tiếp từ Keycloak, hoặc refresh token mà Keycloak sẽ tự kiểm lại ngay sau đó.
 */
public final class TokenClaims {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TokenClaims() {
    }

    /** {@code sid} — mã phiên Keycloak, chung cho access và refresh token của cùng một lần đăng nhập. */
    public static String sessionId(String token) {
        JsonNode payload = payload(token);
        if (payload == null) {
            return null;
        }
        String sid = payload.path("sid").asText(null);
        return (sid == null || sid.isBlank()) ? null : sid;
    }

    private static JsonNode payload(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            return MAPPER.readTree(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }
}
