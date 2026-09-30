package com.example.svgmanager.util;

import java.util.Locale;

/**
 * Slug cho {@code file_key}/{@code part_key} — giữ chữ cái tiếng Việt (mẫu file_key
 * trong V12 giữ nguyên dấu), mọi ký tự khác quy về '-'.
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String s = text.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(s.length());
        boolean dash = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                dash = false;
            } else if (!dash && sb.length() > 0) {
                sb.append('-');
                dash = true;
            }
        }
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == '-') {
            end--;
        }
        return sb.substring(0, end);
    }
}
