package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.DesignVersionResponse;
import com.example.svgmanager.dto.response.SavedDesignResponse;

import java.util.List;

public interface SavedDesignService {

    /** GET /api/v1/designs — bản làm việc của chính user trong token (F-37 · KX-02). */
    List<SavedDesignResponse> getSavedDesigns();

    /** GET /api/v1/designs/{id}/versions — mới nhất trước; bản không tồn tại/không phải của user → 404. */
    List<DesignVersionResponse> getVersions(String designKey);
}
