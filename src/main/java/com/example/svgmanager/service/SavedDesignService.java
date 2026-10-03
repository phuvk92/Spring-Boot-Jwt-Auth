package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.DesignVersionResponse;
import com.example.svgmanager.dto.response.SavedDesignResponse;

import java.util.List;

public interface SavedDesignService {

    /** GET /api/v1/designs — bản làm việc của chính user trong token (F-37 · KX-02). */
    List<SavedDesignResponse> getSavedDesigns();

    /** GET /api/v1/designs/{id}/versions — mới nhất trước; bản không tồn tại/không phải của user → 404. */
    List<DesignVersionResponse> getVersions(String designKey);

    /** POST /api/v1/designs — lần Lưu đầu: tạo bản làm việc và version 1 (F-36). */
    SavedDesignResponse createDesign(com.example.svgmanager.dto.request.DesignSaveRequest request);

    /** PUT /api/v1/designs/{id} — các lần Lưu sau: thêm phiên bản mới (F-36). */
    SavedDesignResponse updateDesign(String designKey, com.example.svgmanager.dto.request.DesignSaveRequest request);

    /** GET /api/v1/designs/{id}/content?version=N — nội dung để mở lại (F-36). */
    com.example.svgmanager.dto.response.DesignContentResponse getDesignContent(String designKey, Integer version);
}
