package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thống kê số lượng tài khoản người dùng theo vai trò trong hệ thống")
public class UserStatsResponse {

    @Schema(description = "Số lượng Quản trị viên (ADMIN)", example = "1")
    private long adminCount;

    @Schema(description = "Số lượng Quản lý đại lý (AGENT)", example = "4")
    private long agentCount;

    @Schema(description = "Số lượng Thợ cắt (USER)", example = "7")
    private long userCount;

    @Schema(description = "Tổng số người dùng hợp lệ", example = "12")
    private long total;

    public UserStatsResponse() {
    }

    public UserStatsResponse(long adminCount, long agentCount, long userCount) {
        this.adminCount = adminCount;
        this.agentCount = agentCount;
        this.userCount = userCount;
        this.total = adminCount + agentCount + userCount;
    }

    public long getAdminCount() {
        return adminCount;
    }

    public void setAdminCount(long adminCount) {
        this.adminCount = adminCount;
    }

    public long getAgentCount() {
        return agentCount;
    }

    public void setAgentCount(long agentCount) {
        this.agentCount = agentCount;
    }

    public long getUserCount() {
        return userCount;
    }

    public void setUserCount(long userCount) {
        this.userCount = userCount;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    // Aliases for convenient frontend binding (stats.admin, stats.agent, stats.user)
    public long getAdmin() {
        return adminCount;
    }

    public long getAgent() {
        return agentCount;
    }

    public long getUser() {
        return userCount;
    }
}
