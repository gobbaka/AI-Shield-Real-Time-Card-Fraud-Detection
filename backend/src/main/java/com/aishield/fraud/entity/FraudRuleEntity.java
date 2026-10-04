package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_rules")
public class FraudRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ruleKey; // e.g. "RULE_HIGH_AMOUNT", "RULE_VELOCITY_SPIKE", "RULE_IMPOSSIBLE_TRAVEL"

    @Column(nullable = false)
    private String ruleName;

    private String description;

    @Column(nullable = false)
    private String category; // "AMOUNT", "VELOCITY", "GEO", "DEVICE", "MERCHANT"

    @Column(nullable = false)
    private Double weight = 1.0;

    @Column(nullable = false)
    private Double thresholdValue = 0.0;

    @Column(nullable = false)
    private Boolean enabled = true;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public FraudRuleEntity() {}

    public FraudRuleEntity(String ruleKey, String ruleName, String description, String category, Double weight, Double thresholdValue, Boolean enabled) {
        this.ruleKey = ruleKey;
        this.ruleName = ruleName;
        this.description = description;
        this.category = category;
        this.weight = weight;
        this.thresholdValue = thresholdValue;
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
