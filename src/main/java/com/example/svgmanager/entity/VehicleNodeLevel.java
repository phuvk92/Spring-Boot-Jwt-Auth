package com.example.svgmanager.entity;

/**
 * Bốn cấp của cây xe (SA-DanhMucXe-v2 §2, chốt board 29/09):
 * Hãng › Dòng xe › Model › Phiên bản.
 */
public enum VehicleNodeLevel {
    BRAND(0),
    SERIES(1),
    MODEL(2),
    SUBTYPE(3);

    private final int depth;

    VehicleNodeLevel(int depth) {
        this.depth = depth;
    }

    public int depth() {
        return depth;
    }

    /** Cấp con hợp lệ ngay dưới cấp này; null khi đã là lá (SUBTYPE). */
    public VehicleNodeLevel childLevel() {
        return this == SUBTYPE ? null : values()[depth + 1];
    }
}
