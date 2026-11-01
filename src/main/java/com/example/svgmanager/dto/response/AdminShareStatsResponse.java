package com.example.svgmanager.dto.response;

public class AdminShareStatsResponse {

    private long totalShares;
    private long adminShares;
    private long clientShares;
    private long activeShares;
    private long revokedShares;
    private long expiredShares;
    private long totalPreviews;
    private long totalDownloads;
    private long totalVerifies;
    private long activeClients;
    private long sharesCreatedToday;
    private long sharesCreatedThisMonth;

    public AdminShareStatsResponse() {
    }

    public long getTotalShares() {
        return totalShares;
    }

    public void setTotalShares(long totalShares) {
        this.totalShares = totalShares;
    }

    public long getAdminShares() {
        return adminShares;
    }

    public void setAdminShares(long adminShares) {
        this.adminShares = adminShares;
    }

    public long getClientShares() {
        return clientShares;
    }

    public void setClientShares(long clientShares) {
        this.clientShares = clientShares;
    }

    public long getActiveShares() {
        return activeShares;
    }

    public void setActiveShares(long activeShares) {
        this.activeShares = activeShares;
    }

    public long getRevokedShares() {
        return revokedShares;
    }

    public void setRevokedShares(long revokedShares) {
        this.revokedShares = revokedShares;
    }

    public long getExpiredShares() {
        return expiredShares;
    }

    public void setExpiredShares(long expiredShares) {
        this.expiredShares = expiredShares;
    }

    public long getTotalPreviews() {
        return totalPreviews;
    }

    public void setTotalPreviews(long totalPreviews) {
        this.totalPreviews = totalPreviews;
    }

    public long getTotalDownloads() {
        return totalDownloads;
    }

    public void setTotalDownloads(long totalDownloads) {
        this.totalDownloads = totalDownloads;
    }

    public long getTotalVerifies() {
        return totalVerifies;
    }

    public void setTotalVerifies(long totalVerifies) {
        this.totalVerifies = totalVerifies;
    }

    public long getActiveClients() {
        return activeClients;
    }

    public void setActiveClients(long activeClients) {
        this.activeClients = activeClients;
    }

    public long getSharesCreatedToday() {
        return sharesCreatedToday;
    }

    public void setSharesCreatedToday(long sharesCreatedToday) {
        this.sharesCreatedToday = sharesCreatedToday;
    }

    public long getSharesCreatedThisMonth() {
        return sharesCreatedThisMonth;
    }

    public void setSharesCreatedThisMonth(long sharesCreatedThisMonth) {
        this.sharesCreatedThisMonth = sharesCreatedThisMonth;
    }
}
