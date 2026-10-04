package com.aishield.fraud;

import com.aishield.fraud.engine.RuleEvaluator;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.entity.TravelNoticeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TravelModeSecurityTest {

    private RuleEvaluator ruleEvaluator;

    @BeforeEach
    void setUp() {
        ruleEvaluator = new RuleEvaluator();
    }

    @Test
    @DisplayName("Without travel notice: Cross-city transaction in short window triggers impossible travel anomaly")
    void testImpossibleTravelWithoutNotice() {
        LocalDateTime now = LocalDateTime.now();
        List<TransactionEntity> history = new ArrayList<>();

        TransactionEntity previousTx = new TransactionEntity();
        previousTx.setLocation("Mumbai");
        previousTx.setAmount(2000.0);
        previousTx.setTimestamp(now.minusMinutes(15));
        previousTx.setDeviceFingerprint("DEV-MUMBAI-01");
        history.add(previousTx);

        RuleEvaluator.RuleEvaluationResult result = ruleEvaluator.evaluate(
                2500.0, "London", "RETAIL", now, "DEV-MUMBAI-01", history, Collections.emptyList()
        );

        assertNotNull(result);
        assertTrue(result.impossibleTravelTriggered, "Should flag impossible travel without travel notice");
        assertFalse(result.travelNoticeApplied, "Travel notice should not be applied");
        assertTrue(result.ruleScore >= 50.0, "Rule score should include 50 point penalty for impossible travel");
        assertTrue(result.triggeredReasons.stream().anyMatch(r -> r.contains("Impossible travel detected")),
                "Reasons must mention impossible travel detected");
    }

    @Test
    @DisplayName("With active travel notice: Geo-velocity anomaly is suppressed and reasons record notice active")
    void testImpossibleTravelSuppressedWithActiveNotice() {
        LocalDateTime now = LocalDateTime.now();
        List<TransactionEntity> history = new ArrayList<>();

        TransactionEntity previousTx = new TransactionEntity();
        previousTx.setLocation("Mumbai");
        previousTx.setAmount(2000.0);
        previousTx.setTimestamp(now.minusMinutes(15));
        previousTx.setDeviceFingerprint("DEV-MUMBAI-01");
        history.add(previousTx);

        TravelNoticeEntity activeNotice = new TravelNoticeEntity(
                null,
                "rahul@example.com",
                "**** **** **** 4721",
                "United Kingdom",
                "London",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(5)
        );

        RuleEvaluator.RuleEvaluationResult result = ruleEvaluator.evaluate(
                2500.0, "London", "RETAIL", now, "DEV-MUMBAI-01", history, List.of(activeNotice)
        );

        assertNotNull(result);
        assertFalse(result.impossibleTravelTriggered, "Impossible travel must be suppressed when active travel notice exists");
        assertTrue(result.travelNoticeApplied, "Result must indicate travel notice was applied");
        assertEquals(0.0, result.ruleScore, "Rule score should be 0 because velocity was suppressed and amount is normal");
        assertTrue(result.triggeredReasons.stream().anyMatch(r -> r.contains("Geo-Velocity Anomaly Suppressed")),
                "Reasons must explicitly indicate Geo-Velocity Anomaly Suppressed");
    }
}
