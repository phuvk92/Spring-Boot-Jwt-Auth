package com.example.svgmanager.entity;

/**
 * Kết quả một job cắt — khớp enum `outcome` của schema CutJob trong openapi v0.3.0.
 * `recut` và `misaligned` KHÔNG được gộp: cắt lại là quyết định của thợ, lệch dao là
 * hỏng máy — hai việc khác nhau nên đếm riêng trong stats.
 */
public enum CutOutcome {
    COMPLETED,
    RECUT,
    MISALIGNED;

    /** Giá trị hợp đồng (snake_case, thường) — JSON và DB đều lưu chuỗi này. */
    public String contractValue() {
        return name().toLowerCase();
    }

    public static CutOutcome fromContractValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return CutOutcome.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
