package com.aishield.fraud.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FraudAnalysisResult {

    private int riskScore; // 0 to 100
    private String riskLevel; // "Low", "Medium", "High"
    private String decision; // "APPROVE", "FLAG", "DECLINE"
    private String transactionStatus; // "Approved", "Review", "Blocked"
    private List<String> reasons = new ArrayList<>();
    private Map<String, Object> signals = new HashMap<>();
    private boolean shouldGenerateAlert;

    public FraudAnalysisResult() {}

    public FraudAnalysisResult(int riskScore, String riskLevel, String decision, String transactionStatus, 
                               List<String> reasons, Map<String, Object> signals, boolean shouldGenerateAlert) {
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.decision = decision;
        this.transactionStatus = transactionStatus;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
        this.signals = signals != null ? signals : new HashMap<>();
        this.shouldGenerateAlert = shouldGenerateAlert;
    }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public Map<String, Object> getSignals() { return signals; }
    public void setSignals(Map<String, Object> signals) { this.signals = signals; }

    public boolean isShouldGenerateAlert() { return shouldGenerateAlert; }
    public void setShouldGenerateAlert(boolean shouldGenerateAlert) { this.shouldGenerateAlert = shouldGenerateAlert; }
}
