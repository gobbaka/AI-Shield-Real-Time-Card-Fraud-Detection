package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_verifications", indexes = {
        @Index(name = "idx_verif_req_id", columnList = "verificationRequestId", unique = true),
        @Index(name = "idx_verif_status", columnList = "status"),
        @Index(name = "idx_verif_customer_email", columnList = "customerEmail")
})
public class TransactionVerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String verificationRequestId; // e.g. "VR-10924-X"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionEntity transaction;

    @Column(nullable = false)
    private String transactionRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String customerName;

    private String customerPhone; // Linked mobile number (e.g. "+91 98111 22334")

    @Column(nullable = false)
    private String cardMasked;

    @Column(nullable = false)
    private Double amount;

    private String merchantName;
    private String location;
    private String riskLevel;
    private Integer riskScore;

    @Column(nullable = false)
    private String status; // "PENDING", "OWNER_CONFIRMED", "OWNER_REJECTED", "EXPIRED", "VERIFICATION_FAILED"

    private String smsStatus; // "SENT", "DELIVERED", "FAILED"

    @Column(length = 1000)
    private String smsContent; // Exact SMS message sent to the cardholder's phone

    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    private String verificationMethod; // "WEBAUTHN_PASSKEY", "SMS_STEP_UP_PORTAL", "PLATFORM_BIOMETRIC"

    @Column(nullable = false, length = 128)
    private String challengeToken; // Cryptographic challenge token (replay protection)

    @Column(length = 2000)
    private String securityMetadata; // Client device signature, authenticator metadata, user agent

    private String failureReason;

    @Column(length = 2000)
    private String auditNotes;

    public TransactionVerificationEntity() {}

    public TransactionVerificationEntity(String verificationRequestId, TransactionEntity transaction, 
                                         String transactionRef, UserEntity user, String customerEmail, 
                                         String customerName, String customerPhone, String cardMasked, Double amount, 
                                         String merchantName, String location, String riskLevel, 
                                         Integer riskScore, String status, LocalDateTime expiresAt, 
                                         String challengeToken) {
        this.verificationRequestId = verificationRequestId;
        this.transaction = transaction;
        this.transactionRef = transactionRef;
        this.user = user;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.cardMasked = cardMasked;
        this.amount = amount;
        this.merchantName = merchantName;
        this.location = location;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.status = status;
        this.requestedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.challengeToken = challengeToken;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVerificationRequestId() { return verificationRequestId; }
    public void setVerificationRequestId(String verificationRequestId) { this.verificationRequestId = verificationRequestId; }

    public TransactionEntity getTransaction() { return transaction; }
    public void setTransaction(TransactionEntity transaction) { this.transaction = transaction; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

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

    public String getSmsStatus() { return smsStatus; }
    public void setSmsStatus(String smsStatus) { this.smsStatus = smsStatus; }

    public String getSmsContent() { return smsContent; }
    public void setSmsContent(String smsContent) { this.smsContent = smsContent; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerificationMethod() { return verificationMethod; }
    public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }

    public String getChallengeToken() { return challengeToken; }
    public void setChallengeToken(String challengeToken) { this.challengeToken = challengeToken; }

    public String getSecurityMetadata() { return securityMetadata; }
    public void setSecurityMetadata(String securityMetadata) { this.securityMetadata = securityMetadata; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getAuditNotes() { return auditNotes; }
    public void setAuditNotes(String auditNotes) { this.auditNotes = auditNotes; }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
