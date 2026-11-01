package com.example.svgmanager.dto.response;

import java.time.LocalDateTime;

public class AdminShareActivityResponse {

    private Long id;
    private LocalDateTime timestamp;
    private String shareId;
    private String shareType; // CLIENT_TO_CLIENT hoặc ADMIN_TO_CLIENT
    private Long fileId;
    private String fileName;
    private String action; // CREATE, VERIFY, PREVIEW, DOWNLOAD, REVOKE, EXPIRE
    private String actionDisplay;
    private String actorUsername;
    private String actorRole;
    private String clientSource;
    private String clientRecipient;
    private String dealerName;
    private String result; // SUCCESS / FAILED
    private Integer httpStatus;
    private String clientIp;
    private String requestId;
    private String reason;
    private String details;

    public AdminShareActivityResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getShareType() {
        return shareType;
    }

    public void setShareType(String shareType) {
        this.shareType = shareType;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionDisplay() {
        return actionDisplay;
    }

    public void setActionDisplay(String actionDisplay) {
        this.actionDisplay = actionDisplay;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getClientSource() {
        return clientSource;
    }

    public void setClientSource(String clientSource) {
        this.clientSource = clientSource;
    }

    public String getClientRecipient() {
        return clientRecipient;
    }

    public void setClientRecipient(String clientRecipient) {
        this.clientRecipient = clientRecipient;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
