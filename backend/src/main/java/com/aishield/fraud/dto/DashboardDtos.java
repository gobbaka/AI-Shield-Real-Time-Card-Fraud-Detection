package com.aishield.fraud.dto;

import java.util.List;

public class DashboardDtos {

    public static class DashboardSummaryDto {
        private long totalTransactions;
        private long fraudAlertCount;
        private double safeTransactionRate;
        private double trustScore;
        private int modelAccuracy;
        private List<TransactionDtos.TransactionResponse> recentTransactions;

        public DashboardSummaryDto() {}

        public long getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }
        public long getFraudAlertCount() { return fraudAlertCount; }
        public void setFraudAlertCount(long fraudAlertCount) { this.fraudAlertCount = fraudAlertCount; }
        public double getSafeTransactionRate() { return safeTransactionRate; }
        public void setSafeTransactionRate(double safeTransactionRate) { this.safeTransactionRate = safeTransactionRate; }
        public double getTrustScore() { return trustScore; }
        public void setTrustScore(double trustScore) { this.trustScore = trustScore; }
        public int getModelAccuracy() { return modelAccuracy; }
        public void setModelAccuracy(int modelAccuracy) { this.modelAccuracy = modelAccuracy; }
        public List<TransactionDtos.TransactionResponse> getRecentTransactions() { return recentTransactions; }
        public void setRecentTransactions(List<TransactionDtos.TransactionResponse> recentTransactions) { this.recentTransactions = recentTransactions; }
    }

    public static class SimulationRequest {
        private String transactionId;
        private Double amount;
        private String customerName;
        private String location;
        private String merchantCategory;

        public SimulationRequest() {}

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getMerchantCategory() { return merchantCategory; }
        public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }
    }

    public static class SimulationResponse {
        private String transactionId;
        private Double amount;
        private String customer;
        private String location;
        private Integer riskScore;
        private String riskLevel;
        private String decision;
        private String status;
        private List<String> reasons;
        private String message;

        public SimulationResponse() {}

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getCustomer() { return customer; }
        public void setCustomer(String customer) { this.customer = customer; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public Integer getRiskScore() { return riskScore; }
        public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<String> getReasons() { return reasons; }
        public void setReasons(List<String> reasons) { this.reasons = reasons; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
