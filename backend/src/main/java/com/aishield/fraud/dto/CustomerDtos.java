package com.aishield.fraud.dto;

import java.util.ArrayList;
import java.util.List;

public class CustomerDtos {

    public static class CustomerResponse {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private Boolean mobileVerified;
        private String card; // e.g. "****4582"
        private Integer transactions;
        private Integer fraudScore;
        private String status; // "Low", "Medium", "High"
        private List<String> locations = new ArrayList<>();

        public CustomerResponse() {}

        public CustomerResponse(Long id, String name, String email, String phone, Boolean mobileVerified, String card, Integer transactions, Integer fraudScore, String status, List<String> locations) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.mobileVerified = mobileVerified;
            this.card = card;
            this.transactions = transactions;
            this.fraudScore = fraudScore;
            this.status = status;
            this.locations = locations != null ? locations : new ArrayList<>();
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Boolean getMobileVerified() { return mobileVerified; }
        public void setMobileVerified(Boolean mobileVerified) { this.mobileVerified = mobileVerified; }
        public String getCard() { return card; }
        public void setCard(String card) { this.card = card; }
        public Integer getTransactions() { return transactions; }
        public void setTransactions(Integer transactions) { this.transactions = transactions; }
        public Integer getFraudScore() { return fraudScore; }
        public void setFraudScore(Integer fraudScore) { this.fraudScore = fraudScore; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<String> getLocations() { return locations; }
        public void setLocations(List<String> locations) { this.locations = locations; }
    }

    public static class CardResponse {
        private Long id;
        private String cardHolder;
        private String cardNumberMasked;
        private String cardType;
        private String status; // "ACTIVE", "BLOCKED"
        private Double dailyLimit;
        private String phone;
        private Boolean mobileVerified;

        public CardResponse() {}

        public CardResponse(Long id, String cardHolder, String cardNumberMasked, String cardType, String status, Double dailyLimit, String phone, Boolean mobileVerified) {
            this.id = id;
            this.cardHolder = cardHolder;
            this.cardNumberMasked = cardNumberMasked;
            this.cardType = cardType;
            this.status = status;
            this.dailyLimit = dailyLimit;
            this.phone = phone;
            this.mobileVerified = mobileVerified;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCardHolder() { return cardHolder; }
        public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }
        public String getCardNumberMasked() { return cardNumberMasked; }
        public void setCardNumberMasked(String cardNumberMasked) { this.cardNumberMasked = cardNumberMasked; }
        public String getCardType() { return cardType; }
        public void setCardType(String cardType) { this.cardType = cardType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Double getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(Double dailyLimit) { this.dailyLimit = dailyLimit; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Boolean getMobileVerified() { return mobileVerified; }
        public void setMobileVerified(Boolean mobileVerified) { this.mobileVerified = mobileVerified; }
    }

    public static class LimitUpdateRequest {
        private Double limit;
        public LimitUpdateRequest() {}
        public LimitUpdateRequest(Double limit) { this.limit = limit; }
        public Double getLimit() { return limit; }
        public void setLimit(Double limit) { this.limit = limit; }
        public Double getDailyLimit() { return limit; }
        public void setDailyLimit(Double dailyLimit) { this.limit = dailyLimit; }
    }

    public static class UpdateLimitRequest {
        private Double limit;
        public UpdateLimitRequest() {}
        public UpdateLimitRequest(Double limit) { this.limit = limit; }
        public Double getLimit() { return limit; }
        public void setLimit(Double limit) { this.limit = limit; }
        public Double getDailyLimit() { return limit; }
        public void setDailyLimit(Double dailyLimit) { this.limit = dailyLimit; }
    }

    public static class DisputeRequest {
        private String transactionRef;
        private String reason;

        public DisputeRequest() {}
        public DisputeRequest(String transactionRef, String reason) {
            this.transactionRef = transactionRef;
            this.reason = reason;
        }

        public String getTransactionRef() { return transactionRef; }
        public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class DisputeResponse {
        private String disputeId;
        private String transactionRef;
        private String status;
        private String message;
        private String filedAt;

        public DisputeResponse() {}
        public DisputeResponse(String disputeId, String transactionRef, String status, String message, String filedAt) {
            this.disputeId = disputeId;
            this.transactionRef = transactionRef;
            this.status = status;
            this.message = message;
            this.filedAt = filedAt;
        }

        public String getDisputeId() { return disputeId; }
        public void setDisputeId(String disputeId) { this.disputeId = disputeId; }
        public String getTransactionRef() { return transactionRef; }
        public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getFiledAt() { return filedAt; }
        public void setFiledAt(String filedAt) { this.filedAt = filedAt; }
    }
}
