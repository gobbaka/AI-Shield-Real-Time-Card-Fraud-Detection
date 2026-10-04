package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_audit_logs", indexes = {
        @Index(name = "idx_audit_tx_ref", columnList = "transactionRef"),
        @Index(name = "idx_audit_event_type", columnList = "eventType"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class FraudAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false, length = 64)
    private String eventType;

    private String transactionRef;
    private String cardLast4;
    private Long userId;
    private String customerName;
    private String actionTaken;
    private Integer riskScore;
    private Double mlProbability;

    @Column(length = 2048)
    private String triggeredRules;

    private String clientIp;
    private String userAgent;

    @Column(length = 2048)
    private String details; // Sanitized metadata only - NO plain PAN, CVV, passwords or OTPs

    public FraudAuditLogEntity() {}

    public FraudAuditLogEntity(String eventType, String transactionRef, String cardLast4, Long userId,
                               String customerName, String actionTaken, Integer riskScore, Double mlProbability,
                               String triggeredRules, String clientIp, String userAgent, String details) {
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
        this.transactionRef = transactionRef;
        this.cardLast4 = cardLast4;
        this.userId = userId;
        this.customerName = customerName;
        this.actionTaken = actionTaken;
        this.riskScore = riskScore;
        this.mlProbability = mlProbability;
        this.triggeredRules = triggeredRules;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.details = details;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public Double getMlProbability() { return mlProbability; }
    public void setMlProbability(Double mlProbability) { this.mlProbability = mlProbability; }

    public String getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(String triggeredRules) { this.triggeredRules = triggeredRules; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
