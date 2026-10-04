package com.aishield.fraud.engine;

import com.aishield.fraud.entity.SystemSettingsEntity;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionEngine {

    private final RuleEvaluator ruleEvaluator;
    private final MachineLearningScorer mlScorer;
    private final DecisionResolver decisionResolver;
    private final SystemSettingsRepository settingsRepository;

    public FraudDetectionEngine(RuleEvaluator ruleEvaluator, 
                                MachineLearningScorer mlScorer, 
                                DecisionResolver decisionResolver, 
                                SystemSettingsRepository settingsRepository) {
        this.ruleEvaluator = ruleEvaluator;
        this.mlScorer = mlScorer;
        this.decisionResolver = decisionResolver;
        this.settingsRepository = settingsRepository;
    }

    public FraudAnalysisResult analyze(Double amount, String location, String merchantCategory, 
                                        LocalDateTime timestamp, String deviceFingerprint, 
                                        List<TransactionEntity> userTransactionHistory) {
        return analyze(amount, location, merchantCategory, timestamp, deviceFingerprint, userTransactionHistory, null);
    }

    public FraudAnalysisResult analyze(Double amount, String location, String merchantCategory, 
                                        LocalDateTime timestamp, String deviceFingerprint, 
                                        List<TransactionEntity> userTransactionHistory,
                                        List<com.aishield.fraud.entity.TravelNoticeEntity> activeTravelNotices) {
        // 1. Fetch live system settings
        SystemSettingsEntity settings = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(new SystemSettingsEntity(false, true, true, true, 80));

        int threshold = settings.getThreshold() != null ? settings.getThreshold() : 80;
        boolean autoBlock = settings.getAutoBlock() != null ? settings.getAutoBlock() : true;
        boolean aiEnabled = settings.getAiDetection() != null ? settings.getAiDetection() : true;

        // 2. Rule evaluation
        RuleEvaluator.RuleEvaluationResult ruleResult = ruleEvaluator.evaluate(
                amount, location, merchantCategory, timestamp, deviceFingerprint, userTransactionHistory, activeTravelNotices
        );

        // 3. ML Scoring via ONNX Runtime (if enabled)
        int historySize = (userTransactionHistory != null) ? userTransactionHistory.size() : 0;
        double mlScore;
        MachineLearningScorer.MLScoringResult mlResult = null;
        if (aiEnabled) {
            mlResult = mlScorer.computeMLScore(
                    amount, location, merchantCategory, timestamp, historySize, 
                    ruleResult.ruleScore, ruleResult, userTransactionHistory
            );
            mlScore = mlResult.mlScore;
        } else {
            mlScore = ruleResult.ruleScore;
        }

        // 4. Decision resolution
        boolean hardRuleTriggered = ruleResult.impossibleTravelTriggered || 
                                   (ruleResult.highVelocityTriggered && ruleResult.amountAnomalyTriggered);

        FraudAnalysisResult finalResult = decisionResolver.resolve(
                ruleResult.ruleScore, mlScore, threshold, autoBlock, 
                ruleResult.triggeredReasons, hardRuleTriggered
        );

        finalResult.getSignals().put("aiDetectionEnabled", aiEnabled);
        finalResult.getSignals().put("historicalTransactionsCount", historySize);
        finalResult.getSignals().put("coldStart", historySize <= 1);
        if (mlResult != null) {
            finalResult.getSignals().put("mlProbability", mlResult.mlProbability);
            finalResult.getSignals().put("modelVersion", mlResult.modelVersion);
            finalResult.getSignals().put("modelAlgorithm", mlResult.algorithm);
            finalResult.getSignals().put("modelConfidence", mlResult.modelConfidence);
            finalResult.getSignals().put("onnxInferenceExecuted", mlResult.onnxInferenceExecuted);
            finalResult.getSignals().put("featureVector", mlResult.featureVector);
        }

        return finalResult;
    }
}
