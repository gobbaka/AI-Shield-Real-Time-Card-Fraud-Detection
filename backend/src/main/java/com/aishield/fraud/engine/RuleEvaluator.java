package com.aishield.fraud.engine;

import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.entity.TravelNoticeEntity;
import com.aishield.fraud.repository.FraudRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class RuleEvaluator {

    private final FraudRuleRepository ruleRepository;

    public RuleEvaluator() {
        this.ruleRepository = null;
    }

    @Autowired(required = false)
    public RuleEvaluator(FraudRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    private double getWeight(String ruleKey, double defaultWeight) {
        if (ruleRepository != null) {
            try {
                return ruleRepository.findByRuleKey(ruleKey)
                        .map(r -> Boolean.TRUE.equals(r.getEnabled()) ? (r.getWeight() != null ? r.getWeight() : 1.0) : 0.0)
                        .orElse(defaultWeight);
            } catch (Exception e) {
                return defaultWeight;
            }
        }
        return defaultWeight;
    }

    private boolean isRuleEnabled(String ruleKey) {
        if (ruleRepository != null) {
            try {
                return ruleRepository.findByRuleKey(ruleKey)
                        .map(r -> Boolean.TRUE.equals(r.getEnabled()))
                        .orElse(true);
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }

    public static class RuleEvaluationResult {
        public double ruleScore = 0.0;
        public List<String> triggeredReasons = new ArrayList<>();
        public boolean highVelocityTriggered = false;
        public boolean impossibleTravelTriggered = false;
        public boolean amountAnomalyTriggered = false;
        public boolean travelNoticeApplied = false;
    }

    public RuleEvaluationResult evaluate(Double amount, String location, String merchantCategory, 
                                         LocalDateTime timestamp, String deviceFingerprint, 
                                         List<TransactionEntity> previousTransactions) {
        return evaluate(amount, location, merchantCategory, timestamp, deviceFingerprint, previousTransactions, null);
    }

    public RuleEvaluationResult evaluate(Double amount, String location, String merchantCategory, 
                                         LocalDateTime timestamp, String deviceFingerprint, 
                                         List<TransactionEntity> previousTransactions,
                                         List<TravelNoticeEntity> activeTravelNotices) {
        RuleEvaluationResult result = new RuleEvaluationResult();
        double score = 0.0;

        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }

        // 1. Amount Threshold & Anomaly Rules
        double amountWeight = getWeight("RULE_HIGH_AMOUNT", 1.0);
        if (isRuleEnabled("RULE_HIGH_AMOUNT") && amountWeight > 0.0 && amount != null) {
            if (amount >= 200000.0) {
                score += 45.0 * amountWeight;
                result.triggeredReasons.add("Critical amount threshold exceeded (> ₹2,00,000)");
                result.amountAnomalyTriggered = true;
            } else if (amount >= 100000.0) {
                score += 30.0 * amountWeight;
                result.triggeredReasons.add("High transaction amount (₹1,00,000 - ₹2,00,000)");
                result.amountAnomalyTriggered = true;
            } else if (amount >= 50000.0) {
                score += 15.0 * amountWeight;
                result.triggeredReasons.add("Elevated transaction amount (> ₹50,000)");
            }

            // Historical Average Check (if historical data exists)
            if (previousTransactions != null && !previousTransactions.isEmpty()) {
                double avgAmount = previousTransactions.stream()
                        .mapToDouble(TransactionEntity::getAmount)
                        .average()
                        .orElse(amount);

                if (avgAmount > 0 && amount >= (avgAmount * 3.5)) {
                    score += 25.0 * amountWeight;
                    result.triggeredReasons.add(String.format("Amount exceeds 3.5x customer historical baseline (Avg: ₹%.0f)", avgAmount));
                    result.amountAnomalyTriggered = true;
                }
            }
        }

        // 2. Velocity Rules (Multiple swipes in short time window)
        double velocityWeight = getWeight("RULE_VELOCITY_SPIKE", 1.0);
        if (isRuleEnabled("RULE_VELOCITY_SPIKE") && velocityWeight > 0.0 && previousTransactions != null && !previousTransactions.isEmpty()) {
            LocalDateTime tenMinutesAgo = timestamp.minusMinutes(10);
            long recentCount = previousTransactions.stream()
                    .filter(t -> t.getTimestamp() != null && t.getTimestamp().isAfter(tenMinutesAgo))
                    .count();

            if (recentCount >= 3) {
                score += 40.0 * velocityWeight;
                result.triggeredReasons.add(String.format("High transaction velocity detected (%d swipes in last 10 minutes)", recentCount + 1));
                result.highVelocityTriggered = true;
            } else if (recentCount >= 1) {
                score += 15.0 * velocityWeight;
                result.triggeredReasons.add("Rapid consecutive transaction activity detected");
            }
        }

        // 3. Geolocation & Impossible Travel Rules (With Travel Notice Dynamic Suppression)
        double travelWeight = getWeight("RULE_IMPOSSIBLE_TRAVEL", 1.2);
        if (previousTransactions != null && !previousTransactions.isEmpty() && location != null) {
            TransactionEntity lastTx = previousTransactions.get(0);
            if (lastTx.getLocation() != null && !lastTx.getLocation().equalsIgnoreCase(location)) {
                // Check if cardholder has active Travel Notice for this destination
                boolean hasActiveTravelNotice = false;
                if (activeTravelNotices != null && !activeTravelNotices.isEmpty()) {
                    hasActiveTravelNotice = activeTravelNotices.stream()
                            .anyMatch(n -> (n.getDestinationCity() != null && n.getDestinationCity().equalsIgnoreCase(location))
                                    || (n.getDestinationCountry() != null && n.getDestinationCountry().equalsIgnoreCase(location)));
                }

                if (hasActiveTravelNotice) {
                    result.triggeredReasons.add(String.format("Transaction location matches active Cardholder Travel Mode notice (%s) — Geo-Velocity Anomaly Suppressed", location));
                    result.travelNoticeApplied = true;
                } else if (isRuleEnabled("RULE_IMPOSSIBLE_TRAVEL") && travelWeight > 0.0 && lastTx.getTimestamp() != null) {
                    long minutesBetween = Math.abs(Duration.between(lastTx.getTimestamp(), timestamp).toMinutes());
                    double normalizedTravelMult = travelWeight / 1.2;
                    if (minutesBetween < 60) {
                        score += 50.0 * normalizedTravelMult;
                        result.triggeredReasons.add(String.format("Impossible travel detected: Used in %s only %d minutes after %s", 
                                location, minutesBetween, lastTx.getLocation()));
                        result.impossibleTravelTriggered = true;
                    } else if (minutesBetween < 180) {
                        score += 20.0 * normalizedTravelMult;
                        result.triggeredReasons.add(String.format("Cross-city transaction within short interval (%s -> %s in %d mins)", 
                                lastTx.getLocation(), location, minutesBetween));
                    }
                }
            }
        }

        // 4. Merchant Category Risk
        double merchantWeight = getWeight("RULE_RISKY_MERCHANT", 1.0);
        if (isRuleEnabled("RULE_RISKY_MERCHANT") && merchantWeight > 0.0 && merchantCategory != null) {
            String upper = merchantCategory.toUpperCase();
            if (upper.contains("CRYPTO") || upper.contains("GAMBLING") || upper.contains("CASINO") || upper.contains("DARKNET")) {
                score += 30.0 * merchantWeight;
                result.triggeredReasons.add("High-risk merchant category detected: " + merchantCategory);
            } else if (upper.contains("JEWELRY") || upper.contains("FOREX") || upper.contains("WIRE")) {
                score += 15.0 * merchantWeight;
                result.triggeredReasons.add("Elevated risk merchant category: " + merchantCategory);
            }
        }

        // 5. Unrecognized Device / Cold-Start Rule
        double deviceWeight = getWeight("RULE_UNKNOWN_DEVICE", 0.8);
        if (isRuleEnabled("RULE_UNKNOWN_DEVICE") && deviceWeight > 0.0 && previousTransactions != null && !previousTransactions.isEmpty() && deviceFingerprint != null) {
            boolean knownDevice = previousTransactions.stream()
                    .anyMatch(t -> deviceFingerprint.equalsIgnoreCase(t.getDeviceFingerprint()));
            if (!knownDevice) {
                score += 15.0 * (deviceWeight / 0.8);
                result.triggeredReasons.add("Unrecognized device fingerprint / new hardware detected");
            }
        } else if (previousTransactions == null || previousTransactions.isEmpty()) {
            // Cold-start explanation (Customer's initial transaction)
            if (score == 0.0) {
                result.triggeredReasons.add("Initial customer baseline transaction verified");
            }
        }

        // 6. Off-Hours High-Value Anomaly (1:00 AM to 5:00 AM)
        int hour = timestamp.getHour();
        if ((hour >= 1 && hour <= 4) && amount != null && amount >= 30000.0) {
            score += 15.0;
            result.triggeredReasons.add(String.format("Late night high-value transaction processed at %02d:%02d", hour, timestamp.getMinute()));
        }

        result.ruleScore = Math.min(100.0, score);
        return result;
    }
}
