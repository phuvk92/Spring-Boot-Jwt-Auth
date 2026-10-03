package com.example.svgmanager.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Deserializer cho LocalDateTime chấp nhận mọi dạng ISO-8601:
 * - Có offset (ví dụ: +07:00, -05:00) hoặc Z (UTC): quy về múi giờ Asia/Ho_Chi_Minh rồi lấy LocalDateTime.
 * - Không có múi giờ (ví dụ: 2026-10-04T01:10:00): giữ nguyên LocalDateTime như trước.
 */
public class FlexibleIsoLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null) {
            return null;
        }
        text = text.trim();
        if (text.isEmpty()) {
            return null;
        }

        // 1. Thử parse dạng OffsetDateTime (bao gồm offset +07:00, -05:00, Z)
        try {
            OffsetDateTime odt = OffsetDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME);
            return odt.atZoneSameInstant(VIETNAM_ZONE).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Không có offset hoặc không parse được theo OffsetDateTime
        }

        // 2. Thử parse dạng LocalDateTime không có múi giờ (ISO-8601 local)
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Fallback sang ISO_DATE_TIME thông thường
        }

        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ex) {
            return (LocalDateTime) ctxt.handleWeirdStringValue(LocalDateTime.class, text,
                    "Không thể chuyển đổi chuỗi thời gian '%s' sang LocalDateTime: %s", text, ex.getMessage());
        }
    }
}
