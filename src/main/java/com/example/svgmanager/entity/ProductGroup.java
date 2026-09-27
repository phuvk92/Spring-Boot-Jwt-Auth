package com.example.svgmanager.entity;

public enum ProductGroup {
    PPF_EXTERIOR("Ngoại thất (PPF Exterior)", "Ngoại thất", 1L),
    PPF_INTERIOR("Nội thất (PPF Interior)", "Nội thất", 2L),
    WINDOW_FILM("Window Film", "Phim cách nhiệt", 3L);

    private final String displayName;
    private final String vietnameseLabel;
    private final Long defaultCategoryId;

    ProductGroup(String displayName, String vietnameseLabel, Long defaultCategoryId) {
        this.displayName = displayName;
        this.vietnameseLabel = vietnameseLabel;
        this.defaultCategoryId = defaultCategoryId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVietnameseLabel() {
        return vietnameseLabel;
    }

    public Long getDefaultCategoryId() {
        return defaultCategoryId;
    }

    public static ProductGroup fromCategoryId(Long categoryId) {
        if (categoryId == null) return null;
        for (ProductGroup pg : values()) {
            if (pg.getDefaultCategoryId().equals(categoryId)) {
                return pg;
            }
        }
        return null;
    }
}
