package com.aishield.fraud.engine;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DecisionResolver {

    public FraudAnalysisResult resolve(double ruleScore, double mlScore, int threshold, 
                                        boolean autoBlock, List<String> reasons, 
                                        boolean hardBlockRuleTriggered) {
        // Weighted composite score: 50% Rule Engine + 50% ML Model
        double composite = (0.50 * ruleScore) + (0.50 * mlScore);

        // If hard rules triggered (impossible travel or severe velocity), ensure high score
        if (hardBlockRuleTriggered && composite < threshold) {
            composite = Math.max(composite, (double) threshold + 5.0);
        }

        int finalScore = (int) Math.round(Math.min(100.0, Math.max(0.0, composite)));

        String riskLevel;
        String decision;
        String transactionStatus;
        boolean shouldGenerateAlert;

        if (reasons == null) {
            reasons = new java.util.ArrayList<>();
        } else {
            reasons = new java.util.ArrayList<>(reasons);
        }

        if (mlScore >= 65.0) {
            reasons.add(String.format("AI Machine Learning Model detected elevated fraud probability (ML Risk: %.0f%%)", mlScore));
        }

        if (finalScore >= threshold) {
            riskLevel = "High";
            decision = autoBlock ? "DECLINE" : "FLAG";
            transactionStatus = "Blocked";
            shouldGenerateAlert = true;
        } else if (finalScore >= 40) {
            riskLevel = "Medium";
            decision = "FLAG";
            transactionStatus = "Review";
            shouldGenerateAlert = true;
        } else {
            riskLevel = "Low";
            decision = "APPROVE";
            transactionStatus = "Approved";
            shouldGenerateAlert = false;
        }

        FraudAnalysisResult result = new FraudAnalysisResult();
        result.setRiskScore(finalScore);
        result.setRiskLevel(riskLevel);
        result.setDecision(decision);
        result.setTransactionStatus(transactionStatus);
        result.setReasons(reasons);
        result.setShouldGenerateAlert(shouldGenerateAlert);

        result.getSignals().put("ruleScore", Math.round(ruleScore));
        result.getSignals().put("mlScore", Math.round(mlScore));
        result.getSignals().put("thresholdUsed", threshold);

        return result;
    }
}
