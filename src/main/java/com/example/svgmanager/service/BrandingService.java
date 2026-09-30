package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.BrandingResponse;

public interface BrandingService {

    /** Thương hiệu của đại lý mà user đang đăng nhập thuộc về (BR-10). Không bao giờ ném lỗi
     *  nghiệp vụ — user độc lập hay đại lý chưa cấu hình đều nhận bản mặc định Pcut (BR-30, BR-32). */
    BrandingResponse getBrandingForCurrentUser();
}
