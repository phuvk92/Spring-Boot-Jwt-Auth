package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kết quả đối soát số dư Quota với Sổ cái giao dịch")
public class QuotaReconciliationResponse {

    private Long dealerId;
    private String dealerName;
    private Integer tableRemainingCuts;
    private Integer ledgerRemainingCuts;
    private Integer difference;
    private boolean balanced;
    private String message;

    public QuotaReconciliationResponse() {
    }

    public QuotaReconciliationResponse(Long dealerId, String dealerName, Integer tableRemainingCuts,
                                       Integer ledgerRemainingCuts, Integer difference, boolean balanced, String message) {
        this.dealerId = dealerId;
        this.dealerName = dealerName;
        this.tableRemainingCuts = tableRemainingCuts;
        this.ledgerRemainingCuts = ledgerRemainingCuts;
        this.difference = difference;
        this.balanced = balanced;
        this.message = message;
    }

    public Long getDealerId() { return dealerId; }
    public void setDealerId(Long dealerId) { this.dealerId = dealerId; }
    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }
    public Integer getTableRemainingCuts() { return tableRemainingCuts; }
    public void setTableRemainingCuts(Integer tableRemainingCuts) { this.tableRemainingCuts = tableRemainingCuts; }
    public Integer getLedgerRemainingCuts() { return ledgerRemainingCuts; }
    public void setLedgerRemainingCuts(Integer ledgerRemainingCuts) { this.ledgerRemainingCuts = ledgerRemainingCuts; }
    public Integer getDifference() { return difference; }
    public void setDifference(Integer difference) { this.difference = difference; }
    public boolean isBalanced() { return balanced; }
    public void setBalanced(boolean balanced) { this.balanced = balanced; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
