package com.aishield.fraud.dto;

import jakarta.validation.constraints.NotBlank;

public class VerificationDtos {

    public static class VerificationRequestResponse {
        private String verificationRequestId;
        private String transactionRef;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String smsStatus;
        private String smsContent;
        private String cardMasked;
        private Double amount;
        private String merchantName;
        private String location;
        private String riskLevel;
        private Integer riskScore;
        private String status;
        private String requestedAt;
        private String expiresAt;
        private String challengeToken;

        public VerificationRequestResponse() {}

        public String getVerificationRequestId() { return verificationRequestId; }
        public void setVerificationRequestId(String verificationRequestId) { this.verificationRequestId = verificationRequestId; }
        public String getTransactionRef() { return transactionRef; }
        public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
        public String getSmsStatus() { return smsStatus; }
        public void setSmsStatus(String smsStatus) { this.smsStatus = smsStatus; }
        public String getSmsContent() { return smsContent; }
        public void setSmsContent(String smsContent) { this.smsContent = smsContent; }
        public String getCardMasked() { return cardMasked; }
        public void setCardMasked(String cardMasked) { this.cardMasked = cardMasked; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public Integer getRiskScore() { return riskScore; }
        public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRequestedAt() { return requestedAt; }
        public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
        public String getExpiresAt() { return expiresAt; }
        public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
        public String getChallengeToken() { return challengeToken; }
        public void setChallengeToken(String challengeToken) { this.challengeToken = challengeToken; }
    }

    public static class SendSmsRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        public SendSmsRequest() {}
        public SendSmsRequest(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    public static class StepUpAuthRequest {
        @NotBlank(message = "Challenge token is required")
        private String challengeToken;

        private String verificationMethod = "WEBAUTHN_PASSKEY"; // WEBAUTHN_PASSKEY | PLATFORM_BIOMETRIC_CHALLENGE | SECURE_DEVICE_TOKEN
        
        // Cryptographic authenticator signature / assertion response (Never raw biometrics)
        private String authenticatorData;
        private String clientDataJSON;
        private String signature;
        private String userAgent;

        public StepUpAuthRequest() {}

        public String getChallengeToken() { return challengeToken; }
        public void setChallengeToken(String challengeToken) { this.challengeToken = challengeToken; }
        public String getVerificationMethod() { return verificationMethod; }
        public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }
        public String getAuthenticatorData() { return authenticatorData; }
        public void setAuthenticatorData(String authenticatorData) { this.authenticatorData = authenticatorData; }
        public String getClientDataJSON() { return clientDataJSON; }
        public void setClientDataJSON(String clientDataJSON) { this.clientDataJSON = clientDataJSON; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    }

    public static class RejectVerificationRequest {
        private String reason = "Customer confirmed unauthorized transaction attempt";
        private Boolean blockCard = true;

        public RejectVerificationRequest() {}
        public RejectVerificationRequest(String reason, Boolean blockCard) {
            this.reason = reason;
            this.blockCard = blockCard;
        }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Boolean getBlockCard() { return blockCard; }
        public void setBlockCard(Boolean blockCard) { this.blockCard = blockCard; }
    }

    public static class VerificationActionResponse {
        private String verificationRequestId;
        private String transactionRef;
        private String verificationStatus; // OWNER_CONFIRMED | OWNER_REJECTED | EXPIRED
        private String transactionStatus;  // OWNER_VERIFIED | CUSTOMER_REPORTED_FRAUD | Blocked
        private String message;
        private String auditTimestamp;

        public VerificationActionResponse() {}

        public VerificationActionResponse(String verificationRequestId, String transactionRef, 
                                          String verificationStatus, String transactionStatus, 
                                          String message, String auditTimestamp) {
            this.verificationRequestId = verificationRequestId;
            this.transactionRef = transactionRef;
            this.verificationStatus = verificationStatus;
            this.transactionStatus = transactionStatus;
            this.message = message;
            this.auditTimestamp = auditTimestamp;
        }

        public String getVerificationRequestId() { return verificationRequestId; }
        public void setVerificationRequestId(String verificationRequestId) { this.verificationRequestId = verificationRequestId; }
        public String getTransactionRef() { return transactionRef; }
        public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
        public String getVerificationStatus() { return verificationStatus; }
        public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
        public String getTransactionStatus() { return transactionStatus; }
        public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getAuditTimestamp() { return auditTimestamp; }
        public void setAuditTimestamp(String auditTimestamp) { this.auditTimestamp = auditTimestamp; }
    }

    public static class VerificationHistoryResponse {
        private String id;
        private String requestId;
        private String transactionRef;
        private Double amount;
        private String merchant;
        private String location;
        private String decision;
        private String method;
        private String verifiedAt;
        private String challengeToken;
        private String status;

        public VerificationHistoryResponse() {}

        public VerificationHistoryResponse(String id, String requestId, String transactionRef, Double amount,
                                           String merchant, String location, String decision, String method,
                                           String verifiedAt, String challengeToken, String status) {
            this.id = id;
            this.requestId = requestId;
            this.transactionRef = transactionRef;
            this.amount = amount;
            this.merchant = merchant;
            this.location = location;
            this.decision = decision;
            this.method = method;
            this.verifiedAt = verifiedAt;
            this.challengeToken = challengeToken;
            this.status = status;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getTransactionRef() { return transactionRef; }
        public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getMerchant() { return merchant; }
        public void setMerchant(String merchant) { this.merchant = merchant; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(String verifiedAt) { this.verifiedAt = verifiedAt; }
        public String getChallengeToken() { return challengeToken; }
        public void setChallengeToken(String challengeToken) { this.challengeToken = challengeToken; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
