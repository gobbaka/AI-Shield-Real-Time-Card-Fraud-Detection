package com.aishield.fraud;

import com.aishield.fraud.engine.MachineLearningScorer;
import com.aishield.fraud.engine.RuleEvaluator;
import com.aishield.fraud.entity.TransactionEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MachineLearningScorerTest {

    private MachineLearningScorer mlScorer;

    @BeforeEach
    void setUp() {
        mlScorer = new MachineLearningScorer();
        mlScorer.init();
    }

    @AfterEach
    void tearDown() {
        mlScorer.cleanup();
    }

    @Test
    @DisplayName("Normal transaction should produce low ML fraud probability")
    void testLowRiskMLInference() {
        List<TransactionEntity> history = new ArrayList<>();
        TransactionEntity t1 = new TransactionEntity();
        t1.setAmount(1500.0);
        t1.setLocation("Hyderabad");
        t1.setTimestamp(LocalDateTime.now().minusDays(1));
        history.add(t1);

        RuleEvaluator.RuleEvaluationResult ruleResult = new RuleEvaluator.RuleEvaluationResult();
        ruleResult.ruleScore = 5.0;

        MachineLearningScorer.MLScoringResult result = mlScorer.computeMLScore(
                1200.0, "Hyderabad", "RETAIL",
                LocalDateTime.of(2026, 9, 8, 14, 30),
                1, 5.0, ruleResult, history
        );

        assertNotNull(result);
        assertTrue(result.mlProbability <= 0.35, "Normal transaction ML probability should be low, got: " + result.mlProbability);
        assertTrue(result.mlScore <= 35.0, "Normal transaction ML score should be <= 35.0");
        assertNotNull(result.modelVersion);
        assertNotNull(result.featureVector);
        assertEquals(10, result.featureVector.size(), "Feature vector should contain exactly 10 features");
    }

    @Test
    @DisplayName("High-risk fraud transaction should produce high ML fraud probability")
    void testHighRiskMLInference() {
        List<TransactionEntity> history = new ArrayList<>();
        TransactionEntity prevTx = new TransactionEntity();
        prevTx.setAmount(2000.0);
        prevTx.setLocation("London");
        prevTx.setTimestamp(LocalDateTime.now().minusMinutes(15));
        history.add(prevTx);

        RuleEvaluator.RuleEvaluationResult ruleResult = new RuleEvaluator.RuleEvaluationResult();
        ruleResult.ruleScore = 85.0;
        ruleResult.impossibleTravelTriggered = true;
        ruleResult.highVelocityTriggered = true;
        ruleResult.amountAnomalyTriggered = true;

        MachineLearningScorer.MLScoringResult result = mlScorer.computeMLScore(
                220000.0, "Dubai", "CRYPTO",
                LocalDateTime.of(2026, 9, 8, 3, 15),
                1, 85.0, ruleResult, history
        );

        assertNotNull(result);
        assertTrue(result.mlProbability >= 0.70, "Critical anomaly ML probability should be high, got: " + result.mlProbability);
        assertTrue(result.mlScore >= 70.0, "Critical anomaly ML score should be >= 70.0");
        assertEquals("RandomForest-v3.0-Production", result.modelVersion);
    }
}