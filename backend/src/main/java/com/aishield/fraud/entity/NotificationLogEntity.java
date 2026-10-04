package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs", indexes = {
        @Index(name = "idx_notif_status", columnList = "status"),
        @Index(name = "idx_notif_user_id", columnList = "userId"),
        @Index(name = "idx_notif_tx_id", columnList = "transactionId")
})
public class NotificationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long transactionId;

    @Column(nullable = false)
    private String destination; // Masked recipient phone e.g. "+91 98XXX 22334"

    @Column(nullable = false)
    private String notificationType; // "SMS_OTP", "SMS_TRANSACTION_ALERT", "STEP_UP_VERIFICATION"

    @Column(nullable = false)
    private String status; // "PENDING", "SENT", "DELIVERED", "FAILED", "EXPIRED"

    private String providerName; // "FAST2SMS", "TWILIO", "MOCK"
    private String providerReference; // External message ID from provider API

    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime sentAt;

    public NotificationLogEntity() {}

    public NotificationLogEntity(Long userId, Long transactionId, String destination, String notificationType, String status, String providerName, String providerReference) {
        this.userId = userId;
        this.transactionId = transactionId;
        this.destination = destination;
        this.notificationType = notificationType;
        this.status = status;
        this.providerName = providerName;
        this.providerReference = providerReference;
        this.createdAt = LocalDateTime.now();
        if ("SENT".equalsIgnoreCase(status) || "DELIVERED".equalsIgnoreCase(status)) {
            this.sentAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
