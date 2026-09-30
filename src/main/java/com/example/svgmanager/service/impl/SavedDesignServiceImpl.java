package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.DesignVersionResponse;
import com.example.svgmanager.dto.response.SavedDesignResponse;
import com.example.svgmanager.entity.WorkDesign;
import com.example.svgmanager.entity.WorkDesignVersion;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.WorkDesignRepository;
import com.example.svgmanager.repository.WorkDesignVersionRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.SavedDesignService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SavedDesignServiceImpl implements SavedDesignService {

    private final WorkDesignRepository workDesignRepository;
    private final WorkDesignVersionRepository versionRepository;
    private final CurrentUserService currentUserService;

    public SavedDesignServiceImpl(WorkDesignRepository workDesignRepository,
                                  WorkDesignVersionRepository versionRepository,
                                  CurrentUserService currentUserService) {
        this.workDesignRepository = workDesignRepository;
        this.versionRepository = versionRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedDesignResponse> getSavedDesigns() {
        Long ownerId = currentUserService.getCurrentUserId();
        List<WorkDesign> rows = workDesignRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);

        // versionCount gom một query cho cả trang, tránh N+1 khi bảng lưu nhiều bản.
        List<Long> ids = rows.stream().map(WorkDesign::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] pair : versionRepository.countGroupedByWorkDesignIds(ids)) {
                counts.put((Long) pair[0], (Long) pair[1]);
            }
        }

        return rows.stream().map(d -> toDto(d, counts.getOrDefault(d.getId(), 0L))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignVersionResponse> getVersions(String designKey) {
        Long ownerId = currentUserService.getCurrentUserId();
        // KÈM chủ sở hữu trong cùng điều kiện tra: bản của người khác cũng là 404 —
        // không lộ "bản có tồn tại nhưng không phải của anh".
        WorkDesign design = workDesignRepository.findByDesignKeyAndOwnerId(designKey, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy bản làm việc.", ErrorCodes.DESIGN_NOT_FOUND));

        return versionRepository.findByWorkDesignIdOrderByNumberDesc(design.getId())
                .stream().map(v -> toDto(v, design.getDesignKey())).toList();
    }

    private SavedDesignResponse toDto(WorkDesign d, long versionCount) {
        SavedDesignResponse dto = new SavedDesignResponse();
        dto.setId(d.getDesignKey());
        dto.setName(d.getName());
        dto.setVehicleLabel(d.getVehicleLabel());
        dto.setCategory(d.getCategory());
        dto.setSourceTemplateId(d.getSourceTemplateId());
        dto.setSourceTemplateName(d.getSourceTemplateName());
        dto.setSourceTemplateChanged(d.isSourceTemplateChanged());
        dto.setPartCount(d.getPartCount());
        dto.setFilmUsage(d.getFilmUsage());
        dto.setUpdatedAt(d.getUpdatedAt());
        dto.setCreatedAt(d.getCreatedAt());
        dto.setCutArea(d.getCutArea());
        dto.setHasBeenCut(d.isHasBeenCut());
        dto.setLastSavedByDeviceName(d.getLastSavedByDeviceName());
        dto.setVersionCount((int) versionCount);
        return dto;
    }

    private DesignVersionResponse toDto(WorkDesignVersion v, String designKey) {
        DesignVersionResponse dto = new DesignVersionResponse();
        // Id chuỗi "{designId}-v{n}" — cùng quy ước mock, client đối chiếu được về bản cha.
        dto.setId(designKey + "-v" + v.getNumber());
        dto.setNumber(v.getNumber());
        dto.setSavedAt(v.getSavedAt());
        dto.setSavedByDeviceName(v.getSavedByDeviceName());
        dto.setNote(v.getNote());
        dto.setCurrent(v.isCurrent());
        return dto;
    }
}
