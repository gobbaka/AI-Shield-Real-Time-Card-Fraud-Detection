package com.aishield.fraud;

import com.aishield.fraud.engine.*;
import com.aishield.fraud.entity.SystemSettingsEntity;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.repository.SystemSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FraudDetectionEngineTest {

    private FraudDetectionEngine fraudEngine;
    private SystemSettingsRepository settingsRepository;

    @BeforeEach
    void setUp() {
        RuleEvaluator ruleEvaluator = new RuleEvaluator();
        MachineLearningScorer mlScorer = new MachineLearningScorer();
        mlScorer.init();
        DecisionResolver decisionResolver = new DecisionResolver();
        settingsRepository = Mockito.mock(SystemSettingsRepository.class);

        when(settingsRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(new SystemSettingsEntity(false, true, true, true, 80)));

        fraudEngine = new FraudDetectionEngine(ruleEvaluator, mlScorer, decisionResolver, settingsRepository);
    }

    @Test
    @DisplayName("Legitimate low-amount transaction should produce LOW risk score and APPROVE decision")
    void testLowRiskTransaction() {
        FraudAnalysisResult result = fraudEngine.analyze(
                2500.0, "Hyderabad", "RETAIL", LocalDateTime.now(), "DEV-WIN-CHROME", new ArrayList<>()
        );

        assertNotNull(result);
        assertTrue(result.getRiskScore() < 40, "Risk score should be < 40 for low normal transaction");
        assertEquals("Low", result.getRiskLevel());
        assertEquals("APPROVE", result.getDecision());
        assertEquals("Approved", result.getTransactionStatus());
        assertFalse(result.isShouldGenerateAlert());
    }

    @Test
    @DisplayName("High amount spike and impossible travel should trigger DECLINE and High Risk")
    void testHighRiskImpossibleTravel() {
        List<TransactionEntity> history = new ArrayList<>();
        TransactionEntity prevTx = new TransactionEntity();
        prevTx.setLocation("London");
        prevTx.setTimestamp(LocalDateTime.now().minusMinutes(20)); // 20 mins ago in London
        prevTx.setAmount(10000.0);
        history.add(prevTx);

        // Now transaction in Mumbai for 1.8 Lakhs
        FraudAnalysisResult result = fraudEngine.analyze(
                180000.0, "Mumbai", "CRYPTO", LocalDateTime.now(), "DEV-NEW-IP", history
        );

        assertNotNull(result);
        assertTrue(result.getRiskScore() >= 80, "Risk score should be >= 80 for impossible travel and high amount");
        assertEquals("High", result.getRiskLevel());
        assertEquals("DECLINE", result.getDecision());
        assertEquals("Blocked", result.getTransactionStatus());
        assertTrue(result.isShouldGenerateAlert());
        assertTrue(result.getReasons().stream().anyMatch(r -> r.contains("Impossible travel")));
    }

    @Test
    @DisplayName("Cold-start first transaction without history should be safely analyzed")
    void testColdStartTransaction() {
        // Cold start (empty history)
        FraudAnalysisResult result = fraudEngine.analyze(
                15000.0, "Chennai", "GROCERY", LocalDateTime.now(), "DEV-PHONE", new ArrayList<>()
        );

        assertNotNull(result);
        assertNotNull(result.getDecision());
        assertNotNull(result.getTransactionStatus());
        assertTrue(result.getRiskScore() >= 0 && result.getRiskScore() <= 100);
        assertEquals(Boolean.TRUE, result.getSignals().get("coldStart"));
    }
}
