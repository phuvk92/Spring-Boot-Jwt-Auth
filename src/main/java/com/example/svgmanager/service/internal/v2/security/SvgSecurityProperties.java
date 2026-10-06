package com.example.svgmanager.service.internal.v2.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "app.file-security.svg")
public class SvgSecurityProperties {

    /** Dung lượng file SVG tối đa cho phép (mặc định 20MB) */
    private DataSize maxFileSize = DataSize.ofMegabytes(20);

    /** Độ sâu tối đa của cây XML (chống XML depth attack) */
    private int maxXmlDepth = 100;

    /** Tổng số phần tử XML tối đa trong file SVG (chống XML element explosion) */
    private int maxElements = 100000;

    /** Số thuộc tính tối đa trên mỗi phần tử */
    private int maxAttributesPerElement = 200;

    /** Độ dài tối đa của một thuộc tính (ký tự) */
    private int maxAttributeLength = 65536;

    /** Cấu hình giới hạn tần suất request (Rate Limiting) */
    private RateLimit rateLimit = new RateLimit();

    public static class RateLimit {
        private boolean enabled = true;
        private int maxRequestsPerMinute = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxRequestsPerMinute() {
            return maxRequestsPerMinute;
        }

        public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
        }
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public int getMaxXmlDepth() {
        return maxXmlDepth;
    }

    public void setMaxXmlDepth(int maxXmlDepth) {
        this.maxXmlDepth = maxXmlDepth;
    }

    public int getMaxElements() {
        return maxElements;
    }

    public void setMaxElements(int maxElements) {
        this.maxElements = maxElements;
    }

    public int getMaxAttributesPerElement() {
        return maxAttributesPerElement;
    }

    public void setMaxAttributesPerElement(int maxAttributesPerElement) {
        this.maxAttributesPerElement = maxAttributesPerElement;
    }

    public int getMaxAttributeLength() {
        return maxAttributeLength;
    }

    public void setMaxAttributeLength(int maxAttributeLength) {
        this.maxAttributeLength = maxAttributeLength;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }
}
