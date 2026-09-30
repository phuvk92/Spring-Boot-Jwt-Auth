package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.CreateVehicleNodeRequest;
import com.example.svgmanager.dto.request.UpdateVehicleNodeRequest;
import com.example.svgmanager.dto.response.DeleteVehicleNodeResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.VehicleNodeImpactResponse;
import com.example.svgmanager.dto.response.VehicleNodeResponse;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.entity.VehicleNodeLevel;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.SvgFileVehicleNodeRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.VehicleNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VehicleNodeServiceImpl implements VehicleNodeService {

    private static final Logger log = LoggerFactory.getLogger(VehicleNodeServiceImpl.class);

    private final VehicleNodeRepository vehicleNodeRepository;
    private final SvgFileVehicleNodeRepository linkRepository;
    private final AuditLogService auditLogService;

    public VehicleNodeServiceImpl(VehicleNodeRepository vehicleNodeRepository,
                                  SvgFileVehicleNodeRepository linkRepository,
                                  AuditLogService auditLogService) {
        this.vehicleNodeRepository = vehicleNodeRepository;
        this.linkRepository = linkRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VehicleNodeResponse> getBrandTrees(String q, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size),
                Sort.by("displayOrder").ascending().and(Sort.by("id").ascending()));

        Page<VehicleNode> brands;
        Set<Long> keepIds = null;
        if (StringUtils.hasText(q)) {
            // Chỉ giữ node khớp q và MỌI TỔ TIÊN của nó; hãng của trang là gốc chứa node khớp
            List<Long> matchedIds = vehicleNodeRepository.findIdsByNameContaining(q.trim());
            if (matchedIds.isEmpty()) {
                return PageResponse.of(Page.empty(pageable));
            }
            List<Long> rootIds = vehicleNodeRepository.findRootIdsOf(matchedIds);
            if (rootIds.isEmpty()) {
                return PageResponse.of(Page.empty(pageable));
            }
            brands = vehicleNodeRepository.findByParentIsNullAndIdIn(rootIds, pageable);
            keepIds = new HashSet<>();
        } else {
            brands = vehicleNodeRepository.findByParentIsNull(pageable);
        }

        // Cây nhỏ (vài trăm node) — nạp một lần rồi ghép trong RAM thay vì N+1 query
        List<VehicleNode> all = vehicleNodeRepository.findAll();
        Map<Long, List<VehicleNode>> byParent = new HashMap<>();
        for (VehicleNode n : all) {
            Long pid = n.getParent() != null ? n.getParent().getId() : null;
            byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(n);
        }
        byParent.values().forEach(list -> list.sort(
                Comparator.comparing(VehicleNode::getDisplayOrder).thenComparing(VehicleNode::getId)));

        String needle = StringUtils.hasText(q) ? q.trim().toLowerCase() : null;
        List<VehicleNodeResponse> content = brands.getContent().stream()
                .map(b -> toTree(b, byParent, needle))
                .filter(t -> t != null)
                .collect(Collectors.toList());

        return new PageResponse<>(content, brands.getNumber(), brands.getSize(),
                brands.getTotalElements(), brands.getTotalPages(), brands.isFirst(), brands.isLast());
    }

    /**
     * Ghép cây con của node. Có {@code needle} thì tỉa: giữ node khớp tên hoặc có
     * hậu duệ được giữ (tức đường đi tới node khớp), trả null cho nhánh cắt hết.
     */
    private VehicleNodeResponse toTree(VehicleNode node, Map<Long, List<VehicleNode>> byParent, String needle) {
        List<VehicleNodeResponse> children = new ArrayList<>();
        for (VehicleNode c : byParent.getOrDefault(node.getId(), List.of())) {
            VehicleNodeResponse t = toTree(c, byParent, needle);
            if (t != null) {
                children.add(t);
            }
        }
        boolean selfMatch = needle == null || node.getName().toLowerCase().contains(needle);
        if (needle != null && !selfMatch && children.isEmpty()) {
            return null;
        }
        int childCount = byParent.getOrDefault(node.getId(), List.of()).size();
        return new VehicleNodeResponse(node.getId(), node.getLevel().name(), node.getName(), childCount, children);
    }

    @Override
    @Transactional
    public VehicleNodeResponse create(CreateVehicleNodeRequest request) {
        String name = request.name().trim();
        VehicleNode parent = null;
        VehicleNodeLevel level = VehicleNodeLevel.BRAND;

        if (request.parentId() != null) {
            parent = vehicleNodeRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy node cha với ID: " + request.parentId()));
            level = parent.getLevel().childLevel();
            if (level == null) {
                throw new BadRequestException("Phiên bản (SUBTYPE) là cấp cuối, không tạo được con");
            }
            if (vehicleNodeRepository.existsByParentIdAndNameIgnoreCase(parent.getId(), name)) {
                throw nameTaken(name);
            }
        } else if (vehicleNodeRepository.existsByParentIsNullAndNameIgnoreCase(name)) {
            throw nameTaken(name);
        }

        VehicleNode node = new VehicleNode(parent, level, name, 0);
        try {
            node = vehicleNodeRepository.saveAndFlush(node);
        } catch (DataIntegrityViolationException e) {
            // Race hai request cùng tạo một tên — unique index bắt lại thành 409
            throw nameTaken(name);
        }

        log.info("[VEHICLE_NODE_CREATED] id={}, level={}, name='{}', parentId={}",
                node.getId(), level, name, request.parentId());
        return new VehicleNodeResponse(node.getId(), level.name(), name, 0, List.of());
    }

    @Override
    @Transactional
    public VehicleNodeResponse rename(Long id, UpdateVehicleNodeRequest request) {
        VehicleNode node = vehicleNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy node xe với ID: " + id));

        String name = request.name().trim();
        boolean dup = node.getParent() != null
                ? vehicleNodeRepository.existsByParentIdAndNameIgnoreCase(node.getParent().getId(), name)
                : vehicleNodeRepository.existsByParentIsNullAndNameIgnoreCase(name);
        // Trùng chính tên cũ của mình vẫn hợp lệ (đổi hoa/thường)
        if (dup && !node.getName().equalsIgnoreCase(name)) {
            throw nameTaken(name);
        }

        node.setName(name);
        try {
            node = vehicleNodeRepository.saveAndFlush(node);
        } catch (DataIntegrityViolationException e) {
            throw nameTaken(name);
        }
        return new VehicleNodeResponse(node.getId(), node.getLevel().name(), node.getName(),
                node.getChildren() != null ? node.getChildren().size() : 0, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleNodeImpactResponse impact(Long id) {
        ensureExists(id);
        List<Long> subtreeIds = vehicleNodeRepository.findSubtreeIds(id);
        return new VehicleNodeImpactResponse(
                subtreeIds.size() - 1,
                linkRepository.countDistinctFilesByNodeIds(subtreeIds));
    }

    @Override
    @Transactional
    public DeleteVehicleNodeResponse delete(Long id) {
        VehicleNode node = vehicleNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy node xe với ID: " + id));

        List<Long> subtreeIds = vehicleNodeRepository.findSubtreeIds(id);
        long unlinked = linkRepository.countDistinctFilesByNodeIds(subtreeIds);

        // Gỡ liên kết file trước rồi xoá gốc nhánh — con cháu rơi theo ON DELETE CASCADE.
        // File KHÔNG bị xoá, chỉ mất liên kết (Q4).
        linkRepository.deleteByVehicleNodeIdIn(subtreeIds);
        vehicleNodeRepository.delete(node);

        log.info("[VEHICLE_NODE_DELETED] id={}, deletedNodes={}, unlinkedFiles={}",
                id, subtreeIds.size(), unlinked);
        return new DeleteVehicleNodeResponse(subtreeIds.size(), unlinked);
    }

    private void ensureExists(Long id) {
        if (!vehicleNodeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy node xe với ID: " + id);
        }
    }

    private ConflictException nameTaken(String name) {
        return new ConflictException("Đã có node tên '" + name + "' trong cùng nhánh cha",
                ErrorCodes.NODE_NAME_TAKEN);
    }
}
