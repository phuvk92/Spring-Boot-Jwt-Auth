package com.example.svgmanager.dto.request;

public class AdminRevokeShareRequest {

    private String reason;

    public AdminRevokeShareRequest() {
    }

    public AdminRevokeShareRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
