package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String alertRef; // e.g. "FA1023"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transaction;

    private String transactionRef;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Integer riskScore;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String status; // "Open", "Reviewing", "Resolved", "Blocked"

    @Column(length = 2000)
    private String triggerReasons;

    private String assignedInvestigator;

    @Column(length = 2000)
    private String investigatorNotes;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    public FraudAlertEntity() {}

    public FraudAlertEntity(String alertRef, TransactionEntity transaction, String transactionRef, 
                            String customerName, Double amount, Integer riskScore, String location, 
                            String status, String triggerReasons) {
        this.alertRef = alertRef;
        this.transaction = transaction;
        this.transactionRef = transactionRef;
        this.customerName = customerName;
        this.amount = amount;
        this.riskScore = riskScore;
        this.location = location;
        this.status = status;
        this.triggerReasons = triggerReasons;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAlertRef() { return alertRef; }
    public void setAlertRef(String alertRef) { this.alertRef = alertRef; }

    public TransactionEntity getTransaction() { return transaction; }
    public void setTransaction(TransactionEntity transaction) { this.transaction = transaction; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTriggerReasons() { return triggerReasons; }
    public void setTriggerReasons(String triggerReasons) { this.triggerReasons = triggerReasons; }

    public String getAssignedInvestigator() { return assignedInvestigator; }
    public void setAssignedInvestigator(String assignedInvestigator) { this.assignedInvestigator = assignedInvestigator; }

    public String getInvestigatorNotes() { return investigatorNotes; }
    public void setInvestigatorNotes(String investigatorNotes) { this.investigatorNotes = investigatorNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
