package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transactionRef; // e.g. "#TX10001"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private CardEntity card;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String cardMasked;

    @Column(nullable = false)
    private Double amount;

    private String currency = "INR";

    private String merchantName;

    private String merchantCategory; // e.g. "RETAIL", "TRAVEL", "ELECTRONICS", "CRYPTO"

    @Column(nullable = false)
    private String location; // e.g. "Hyderabad", "Mumbai", "London"

    private String deviceFingerprint;

    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String status; // "Approved", "Review", "Blocked"

    @Column(nullable = false)
    private String risk; // "Low", "Medium", "High"

    @Column(nullable = false)
    private Integer riskScore; // 0 to 100

    @Column(length = 2000)
    private String decisionReasons; // JSON or comma-separated reasons

    private String decision; // "APPROVE", "FLAG", "DECLINE"

    private Double mlProbability;
    private Double ruleScore;
    private Double mlScore;

    public TransactionEntity() {}

    public TransactionEntity(String transactionRef, UserEntity user, CardEntity card, String customerName, 
                             String cardMasked, Double amount, String merchantName, String merchantCategory, 
                             String location, String deviceFingerprint, String ipAddress, LocalDateTime timestamp, 
                             String status, String risk, Integer riskScore, String decisionReasons, String decision) {
        this.transactionRef = transactionRef;
        this.user = user;
        this.card = card;
        this.customerName = customerName;
        this.cardMasked = cardMasked;
        this.amount = amount;
        this.merchantName = merchantName;
        this.merchantCategory = merchantCategory;
        this.location = location;
        this.deviceFingerprint = deviceFingerprint;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = status;
        this.risk = risk;
        this.riskScore = riskScore;
        this.decisionReasons = decisionReasons;
        this.decision = decision;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public CardEntity getCard() { return card; }
    public void setCard(CardEntity card) { this.card = card; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCardMasked() { return cardMasked; }
    public void setCardMasked(String cardMasked) { this.cardMasked = cardMasked; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

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

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRisk() { return risk; }
    public void setRisk(String risk) { this.risk = risk; }

    public String getRiskLevel() { return risk; }
    public void setRiskLevel(String riskLevel) { this.risk = riskLevel; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getDecisionReasons() { return decisionReasons; }
    public void setDecisionReasons(String decisionReasons) { this.decisionReasons = decisionReasons; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public Double getMlProbability() { return mlProbability; }
    public void setMlProbability(Double mlProbability) { this.mlProbability = mlProbability; }

    public Double getRuleScore() { return ruleScore; }
    public void setRuleScore(Double ruleScore) { this.ruleScore = ruleScore; }

    public Double getMlScore() { return mlScore; }
    public void setMlScore(Double mlScore) { this.mlScore = mlScore; }
}
