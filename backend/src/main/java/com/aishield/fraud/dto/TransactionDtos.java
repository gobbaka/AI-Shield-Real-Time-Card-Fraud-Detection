package com.aishield.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class TransactionDtos {

    public static class IngestTransactionRequest {
        private String cardLast4; // e.g. "4721" or "**** 4721"
        private String customerName;
        private Long userId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        private Double amount;

        private String merchantName;
        private String merchantCategory;

        @NotBlank(message = "Location is required")
        private String location;

        private String deviceFingerprint;
        private String ipAddress;

        public IngestTransactionRequest() {}

        public String getCardLast4() { return cardLast4; }
        public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        public String getMerchantCategory() { return merchantCategory; }
        public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getDeviceFingerprint() { return deviceFingerprint; }
        public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    }

    public static class TransactionResponse {
        private String id; // "#TX10001"
        private String customer; // "Rahul Sharma"
        private String card; // "**** **** **** 4721"
        private Double amount; // 25000.0
        private String location; // "Hyderabad"
        private String timestamp; // ISO 8601
        private String risk; // "Low", "Medium", "High"
        private Integer riskScore; // 12
        private String status; // "Approved", "Review", "Blocked", "Verification Required", "OWNER_VERIFIED", "CUSTOMER_REPORTED_FRAUD"
        private String merchantName;
        private String merchantCategory;
        private String decision;
        private List<String> reasons;
        private String verificationRequestId;
        private String verificationStatus;
        private Double mlProbability;
        private Double ruleScore;
        private Double mlScore;
        private String modelVersion;
        private Boolean onnxInferenceExecuted;

        public TransactionResponse() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCustomer() { return customer; }
        public void setCustomer(String customer) { this.customer = customer; }
        public String getCard() { return card; }
        public void setCard(String card) { this.card = card; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk; }
        public Integer getRiskScore() { return riskScore; }
        public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        public String getMerchantCategory() { return merchantCategory; }
        public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }
        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        public List<String> getReasons() { return reasons; }
        public void setReasons(List<String> reasons) { this.reasons = reasons; }
        public String getVerificationRequestId() { return verificationRequestId; }
        public void setVerificationRequestId(String verificationRequestId) { this.verificationRequestId = verificationRequestId; }
        public String getVerificationStatus() { return verificationStatus; }
        public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
        public Double getMlProbability() { return mlProbability; }
        public void setMlProbability(Double mlProbability) { this.mlProbability = mlProbability; }
        public Double getRuleScore() { return ruleScore; }
        public void setRuleScore(Double ruleScore) { this.ruleScore = ruleScore; }
        public Double getMlScore() { return mlScore; }
        public void setMlScore(Double mlScore) { this.mlScore = mlScore; }
        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
        public Boolean getOnnxInferenceExecuted() { return onnxInferenceExecuted; }
        public void setOnnxInferenceExecuted(Boolean onnxInferenceExecuted) { this.onnxInferenceExecuted = onnxInferenceExecuted; }
    }
}
