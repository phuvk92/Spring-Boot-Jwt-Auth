package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.DeviceStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SystemDeviceResponse;

/**
 * Nghiệp vụ danh sách và thống kê thiết bị toàn hệ thống (F-57 · NGO-422).
 */
public interface DeviceManagementService {

    /**
     * Danh sách máy đã đăng ký toàn hệ thống, phân trang và lọc tuỳ chọn.
     * ADMIN: thấy tất cả; AGENT: chỉ thấy user thuộc đại lý của mình.
     *
     * @param q        tìm kiếm theo tên user / họ tên / tên máy / IP
     * @param dealerId lọc theo ID đại lý (AGENT bị ép về đại lý mình)
     * @param status   ACTIVE (mặc định) | REVOKED | ALL
     * @param page     số trang (0-based)
     * @param size     kích thước trang
     * @param sortBy   trường sắp xếp (mặc định lastSeenAt)
     * @param sortDir  hướng sắp xếp (mặc định desc)
     * @return danh sách thiết bị phân trang
     */
    PageResponse<SystemDeviceResponse> getDevices(
            String q,
            Long dealerId,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    /**
     * Bốn số thẻ đầu trang: activeNow, registered, usersAtLimit, staleDevices.
     * ADMIN: toàn hệ thống; AGENT: theo đại lý của mình.
     */
    DeviceStatsResponse getStats();
}
