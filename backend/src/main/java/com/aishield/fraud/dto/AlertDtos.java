package com.aishield.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class AlertDtos {

    public static class AlertResponse {
        private String id; // "FA1023"
        private String transactionId; // "#TX10003"
        private String customer; // "Rahul Sharma"
        private Double amount; // 120000.0
        private Integer riskScore; // 98
        private String location; // "Hyderabad"
        private String status; // "Open", "Reviewing", "Resolved", "Blocked"
        private String createdAt; // "10 min ago" or ISO
        private String triggerReasons;
        private String assignedInvestigator;
        private String investigatorNotes;

        public AlertResponse() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getCustomer() { return customer; }
        public void setCustomer(String customer) { this.customer = customer; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public Integer getRiskScore() { return riskScore; }
        public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getTriggerReasons() { return triggerReasons; }
        public void setTriggerReasons(String triggerReasons) { this.triggerReasons = triggerReasons; }
        public String getAssignedInvestigator() { return assignedInvestigator; }
        public void setAssignedInvestigator(String assignedInvestigator) { this.assignedInvestigator = assignedInvestigator; }
        public String getInvestigatorNotes() { return investigatorNotes; }
        public void setInvestigatorNotes(String investigatorNotes) { this.investigatorNotes = investigatorNotes; }
    }

    public static class UpdateAlertStatusRequest {
        @NotBlank(message = "Status is required")
        private String status; // "Open", "Reviewing", "Resolved", "Blocked"
        private String notes;

        public UpdateAlertStatusRequest() {}
        public UpdateAlertStatusRequest(String status, String notes) {
            this.status = status;
            this.notes = notes;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
