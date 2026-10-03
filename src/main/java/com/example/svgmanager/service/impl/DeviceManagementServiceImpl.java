package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.DeviceStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SystemDeviceResponse;
import com.example.svgmanager.entity.DeviceStatus;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.DeviceManagementService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceManagementServiceImpl implements DeviceManagementService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final int defaultMaxDevices;

    public DeviceManagementServiceImpl(
            UserDeviceRepository userDeviceRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            @Value("${app.device.max-per-user:1}") int defaultMaxDevices
    ) {
        this.userDeviceRepository = userDeviceRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.defaultMaxDevices = Math.max(1, defaultMaxDevices);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SystemDeviceResponse> getDevices(
            String q,
            Long dealerId,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        if (!isAdmin && !isAgent) {
            throw new ForbiddenException("Chỉ ADMIN hoặc AGENT mới có quyền xem danh sách thiết bị");
        }

        // AGENT: dealerId bị ép về đại lý của mình
        Long effectiveDealerId = dealerId;
        if (!isAdmin && isAgent) {
            User currentUser = currentUserService.getCurrentUser();
            effectiveDealerId = currentUser.getDealer() != null ? currentUser.getDealer().getId() : -1L;
        }

        String effectiveSortBy = "lastSeenAt";
        if (StringUtils.hasText(sortBy)) {
            String s = sortBy.trim();
            if ("firstSeenAt".equalsIgnoreCase(s)
                    || "lastSeenAt".equalsIgnoreCase(s)
                    || "deviceName".equalsIgnoreCase(s)
                    || "lastIp".equalsIgnoreCase(s)
                    || "status".equalsIgnoreCase(s)
                    || "revokedAt".equalsIgnoreCase(s)
                    || "id".equalsIgnoreCase(s)
                    || "deviceRegId".equalsIgnoreCase(s)) {
                effectiveSortBy = "deviceRegId".equalsIgnoreCase(s) ? "id" : s;
            }
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, effectiveSortBy).and(Sort.by(Sort.Direction.DESC, "id")));

        String cleanQ = StringUtils.hasText(q) ? q.trim() : null;
        String cleanStatus = StringUtils.hasText(status) ? status.trim() : "ACTIVE";
        final Long scopedDealerId = effectiveDealerId;

        Specification<UserDevice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<UserDevice, User> userJoin = root.join("user", JoinType.INNER);

            // Bỏ qua thiết bị của user đã bị soft-deleted
            predicates.add(cb.or(cb.isNull(userJoin.get("deleted")), cb.isFalse(userJoin.get("deleted"))));

            // Lọc trạng thái (ACTIVE mặc định · REVOKED · ALL)
            if (!"ALL".equalsIgnoreCase(cleanStatus)) {
                if ("REVOKED".equalsIgnoreCase(cleanStatus)) {
                    predicates.add(cb.equal(root.get("status"), DeviceStatus.REVOKED));
                } else {
                    // Mặc định hoặc ACTIVE
                    predicates.add(cb.equal(root.get("status"), DeviceStatus.ACTIVE));
                }
            }

            // Lọc theo đại lý
            if (scopedDealerId != null) {
                predicates.add(cb.equal(userJoin.get("dealer").get("id"), scopedDealerId));
            }

            // Lọc q: tên user / họ tên / tên máy / IP
            if (cleanQ != null) {
                String pattern = "%" + cleanQ.toLowerCase() + "%";
                Predicate usernameMatch = cb.like(cb.lower(userJoin.get("username")), pattern);
                Predicate fullNameMatch = cb.like(cb.lower(userJoin.get("fullName")), pattern);
                Predicate deviceNameMatch = cb.like(cb.lower(root.get("deviceName")), pattern);
                Predicate ipMatch = cb.like(cb.lower(root.get("lastIp")), pattern);
                predicates.add(cb.or(usernameMatch, fullNameMatch, deviceNameMatch, ipMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserDevice> pageResult = userDeviceRepository.findAll(spec, pageable);
        return PageResponse.of(pageResult.map(SystemDeviceResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceStatsResponse getStats() {
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        if (!isAdmin && !isAgent) {
            throw new ForbiddenException("Chỉ ADMIN hoặc AGENT mới có quyền xem thống kê thiết bị");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime activeNowSince = now.minusMinutes(15);
        LocalDateTime staleThreshold = now.minusDays(30);

        if (!isAdmin && isAgent) {
            User currentUser = currentUserService.getCurrentUser();
            Long dealerId = currentUser.getDealer() != null ? currentUser.getDealer().getId() : null;

            if (dealerId == null) {
                return new DeviceStatsResponse(0L, 0L, 0L, 0L);
            }

            long activeNow = userDeviceRepository.countActiveSinceAndDealerId(DeviceStatus.ACTIVE, activeNowSince, dealerId);
            long registered = userDeviceRepository.countByStatusAndDealerId(DeviceStatus.ACTIVE, dealerId);
            long usersAtLimit = userRepository.countUsersAtLimitAndDealerId(DeviceStatus.ACTIVE, defaultMaxDevices, dealerId);
            long staleDevices = userDeviceRepository.countStaleDevicesAndDealerId(DeviceStatus.ACTIVE, staleThreshold, dealerId);

            return new DeviceStatsResponse(activeNow, registered, usersAtLimit, staleDevices);
        }

        // ADMIN: toàn hệ thống
        long activeNow = userDeviceRepository.countActiveSince(DeviceStatus.ACTIVE, activeNowSince);
        long registered = userDeviceRepository.countByStatus(DeviceStatus.ACTIVE);
        long usersAtLimit = userRepository.countUsersAtLimit(DeviceStatus.ACTIVE, defaultMaxDevices);
        long staleDevices = userDeviceRepository.countStaleDevices(DeviceStatus.ACTIVE, staleThreshold);

        return new DeviceStatsResponse(activeNow, registered, usersAtLimit, staleDevices);
    }
}
