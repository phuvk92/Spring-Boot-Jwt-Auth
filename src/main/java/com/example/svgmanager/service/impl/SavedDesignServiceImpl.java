package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.DesignSaveRequest;
import com.example.svgmanager.dto.response.DesignContentResponse;
import com.example.svgmanager.dto.response.DesignVersionResponse;
import com.example.svgmanager.dto.response.SavedDesignResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.entity.WorkDesign;
import com.example.svgmanager.entity.WorkDesignVersion;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.PayloadTooLargeException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.WorkDesignRepository;
import com.example.svgmanager.repository.WorkDesignVersionRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.SavedDesignService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SavedDesignServiceImpl implements SavedDesignService {

    private static final int MAX_PAYLOAD_BYTES = 20 * 1024 * 1024; // 20 MB (F-36)
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WorkDesignRepository workDesignRepository;
    private final WorkDesignVersionRepository versionRepository;
    private final SvgFileRepository svgFileRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final CurrentUserService currentUserService;

    public SavedDesignServiceImpl(WorkDesignRepository workDesignRepository,
                                  WorkDesignVersionRepository versionRepository,
                                  SvgFileRepository svgFileRepository,
                                  UserDeviceRepository userDeviceRepository,
                                  CurrentUserService currentUserService) {
        this.workDesignRepository = workDesignRepository;
        this.versionRepository = versionRepository;
        this.svgFileRepository = svgFileRepository;
        this.userDeviceRepository = userDeviceRepository;
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

    @Override
    @Transactional
    public SavedDesignResponse createDesign(DesignSaveRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Tên bản làm việc không được để trống.", ErrorCodes.DESIGN_INVALID);
        }

        String payloadStr = extractAndValidatePayload(request.getPayload());
        int payloadSize = payloadStr.getBytes(StandardCharsets.UTF_8).length;

        User owner = currentUserService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String deviceName = resolveCurrentDeviceName();

        WorkDesign design = new WorkDesign();
        design.setDesignKey(generateUniqueDesignKey());
        design.setOwner(owner);
        design.setName(request.getName().trim());
        design.setVehicleLabel(request.getVehicleLabel() != null ? request.getVehicleLabel().trim() : null);
        design.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);

        // sourceTemplateId và tên mẫu gốc
        if (StringUtils.hasText(request.getSourceTemplateId())) {
            String tid = request.getSourceTemplateId().trim();
            design.setSourceTemplateId(tid);
            design.setSourceTemplateName(resolveTemplateName(tid));
        }

        extractLayoutMetadata(request.getPayload(), design);

        design.setHasBeenCut(false);
        design.setLastSavedByDeviceName(deviceName);
        design.setCreatedAt(now);
        design.setUpdatedAt(now);

        WorkDesign savedDesign = workDesignRepository.save(design);

        WorkDesignVersion v1 = new WorkDesignVersion();
        v1.setWorkDesign(savedDesign);
        v1.setNumber(1);
        v1.setSavedAt(now);
        v1.setSavedByDeviceName(deviceName);
        v1.setCurrent(true);
        v1.setPayload(payloadStr);
        v1.setPayloadSize(payloadSize);

        versionRepository.save(v1);

        return toDto(savedDesign, 1L);
    }

    @Override
    @Transactional
    public SavedDesignResponse updateDesign(String designKey, DesignSaveRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Tên bản làm việc không được để trống.", ErrorCodes.DESIGN_INVALID);
        }

        String payloadStr = extractAndValidatePayload(request.getPayload());
        int payloadSize = payloadStr.getBytes(StandardCharsets.UTF_8).length;

        Long ownerId = currentUserService.getCurrentUserId();
        WorkDesign design = workDesignRepository.findByDesignKeyAndOwnerId(designKey, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy bản làm việc.", ErrorCodes.DESIGN_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        String deviceName = resolveCurrentDeviceName();

        // Cập nhật siêu dữ liệu (sourceTemplateId và sourceTemplateName giữ nguyên không đổi — F-36 / openapi spec)
        design.setName(request.getName().trim());
        if (request.getVehicleLabel() != null) {
            design.setVehicleLabel(request.getVehicleLabel().trim());
        }
        if (request.getCategory() != null) {
            design.setCategory(request.getCategory().trim());
        }

        extractLayoutMetadata(request.getPayload(), design);

        design.setLastSavedByDeviceName(deviceName);
        design.setUpdatedAt(now);
        workDesignRepository.save(design);

        // Lấy tất cả versions hiện tại để cập nhật is_current = false
        List<WorkDesignVersion> versions = versionRepository.findByWorkDesignIdOrderByNumberDesc(design.getId());
        int nextNumber = versions.isEmpty() ? 1 : versions.getFirst().getNumber() + 1;

        for (WorkDesignVersion v : versions) {
            if (v.isCurrent()) {
                v.setCurrent(false);
                versionRepository.save(v);
            }
        }

        WorkDesignVersion newVersion = new WorkDesignVersion();
        newVersion.setWorkDesign(design);
        newVersion.setNumber(nextNumber);
        newVersion.setSavedAt(now);
        newVersion.setSavedByDeviceName(deviceName);
        newVersion.setCurrent(true);
        newVersion.setPayload(payloadStr);
        newVersion.setPayloadSize(payloadSize);

        versionRepository.save(newVersion);

        int totalVersions = versions.size() + 1;
        return toDto(design, (long) totalVersions);
    }

    @Override
    @Transactional(readOnly = true)
    public DesignContentResponse getDesignContent(String designKey, Integer version) {
        Long ownerId = currentUserService.getCurrentUserId();
        WorkDesign design = workDesignRepository.findByDesignKeyAndOwnerId(designKey, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy bản làm việc.", ErrorCodes.DESIGN_NOT_FOUND));

        WorkDesignVersion targetVersion;
        if (version != null) {
            targetVersion = versionRepository.findByWorkDesignIdAndNumber(design.getId(), version)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy phiên bản " + version + " của bản làm việc.", ErrorCodes.DESIGN_NOT_FOUND));
        } else {
            targetVersion = versionRepository.findByWorkDesignIdAndCurrentTrue(design.getId())
                    .orElseGet(() -> {
                        List<WorkDesignVersion> list = versionRepository.findByWorkDesignIdOrderByNumberDesc(design.getId());
                        if (list.isEmpty()) {
                            throw new ResourceNotFoundException(
                                    "Bản làm việc chưa có phiên bản nào.", ErrorCodes.DESIGN_NOT_FOUND);
                        }
                        return list.getFirst();
                    });
        }

        return new DesignContentResponse(
                design.getDesignKey(),
                targetVersion.getNumber(),
                targetVersion.getSavedAt(),
                targetVersion.getPayload()
        );
    }

    private String extractAndValidatePayload(JsonNode payloadNode) {
        if (payloadNode == null || payloadNode.isNull() || payloadNode.isMissingNode()) {
            throw new BadRequestException("Payload không được để trống.", ErrorCodes.DESIGN_INVALID);
        }

        String payloadStr;
        if (payloadNode.isTextual()) {
            payloadStr = payloadNode.asText();
        } else {
            payloadStr = payloadNode.toString();
        }

        if (!StringUtils.hasText(payloadStr)) {
            throw new BadRequestException("Payload không được để trống.", ErrorCodes.DESIGN_INVALID);
        }

        byte[] bytes = payloadStr.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_PAYLOAD_BYTES) {
            throw new PayloadTooLargeException(
                    "Bản làm việc vượt quá dung lượng tối đa cho phép (20 MB): " + bytes.length + " bytes.",
                    ErrorCodes.DESIGN_TOO_LARGE);
        }

        return payloadStr;
    }

    private void extractLayoutMetadata(JsonNode payloadNode, WorkDesign design) {
        if (payloadNode != null && payloadNode.isObject()) {
            // cutArea: { widthMm, heightMm }
            JsonNode cutAreaNode = payloadNode.get("cutArea");
            if (cutAreaNode != null && cutAreaNode.isObject()) {
                JsonNode wNode = cutAreaNode.get("widthMm");
                JsonNode hNode = cutAreaNode.get("heightMm");
                if (wNode != null && hNode != null) {
                    design.setCutArea(formatNumber(wNode.asDouble()) + " × " + formatNumber(hNode.asDouble()));
                }
            }

            // parts: [ ... ]
            JsonNode partsNode = payloadNode.get("parts");
            if (partsNode != null && partsNode.isArray()) {
                design.setPartCount(partsNode.size());
                double maxFilmUsageMm = 0.0;
                for (JsonNode part : partsNode) {
                    double y = part.path("yMm").asDouble(0.0);
                    double h = part.path("heightMm").asDouble(0.0);
                    if (y + h > maxFilmUsageMm) {
                        maxFilmUsageMm = y + h;
                    }
                }
                if (partsNode.size() == 0) {
                    design.setFilmUsage("0 m");
                } else {
                    double meters = maxFilmUsageMm / 1000.0;
                    design.setFilmUsage(String.format(java.util.Locale.US, "%.2f m", meters).replace('.', ','));
                }
            }
        }
    }

    private String formatNumber(double val) {
        if (val == (long) val) {
            return String.format(java.util.Locale.US, "%d", (long) val);
        }
        return String.format(java.util.Locale.US, "%.1f", val).replaceAll("\\.?0+$", "");
    }

    private String resolveCurrentDeviceName() {
        return currentUserService.getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("sid"))
                .filter(StringUtils::hasText)
                .flatMap(userDeviceRepository::findByKeycloakSessionId)
                .map(UserDevice::getDeviceName)
                .orElse(null);
    }

    private String resolveTemplateName(String templateId) {
        if (!StringUtils.hasText(templateId)) {
            return null;
        }
        // Thử theo fileKey trước (vd "tpl-abarth-695-2024" hoặc slug)
        return svgFileRepository.findByFileKey(templateId)
                .map(f -> StringUtils.hasText(f.getDisplayName()) ? f.getDisplayName() : f.getOriginalFilename())
                .orElseGet(() -> {
                    try {
                        long fid = Long.parseLong(templateId);
                        return svgFileRepository.findById(fid)
                                .map(f -> StringUtils.hasText(f.getDisplayName()) ? f.getDisplayName() : f.getOriginalFilename())
                                .orElse(null);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                });
    }

    private String generateUniqueDesignKey() {
        for (int i = 0; i < 10; i++) {
            int num = 1000 + RANDOM.nextInt(9000);
            String key = "wd-" + num;
            if (!workDesignRepository.existsByDesignKey(key)) {
                return key;
            }
        }
        return "wd-" + System.currentTimeMillis();
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

