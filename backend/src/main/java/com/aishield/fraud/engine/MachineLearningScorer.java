package com.aishield.fraud.engine;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.aishield.fraud.entity.TransactionEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class MachineLearningScorer {

    private static final Logger log = LoggerFactory.getLogger(MachineLearningScorer.class);

    private OrtEnvironment env;
    private OrtSession session;
    private boolean modelLoaded = false;

    private String modelVersion = "RandomForest-v3.0-Production";
    private String algorithm = "Random Forest Ensemble (Balanced Subsample)";
    private double modelConfidence = 1.0;
    private double prAuc = 1.0;
    private double rocAuc = 1.0;

    public static class MLScoringResult {
        public double mlProbability; // 0.0 to 1.0
        public double mlScore; // 0.0 to 100.0
        public String modelVersion = "RandomForest-v3.0-Production";
        public String algorithm = "Random Forest Ensemble";
        public double modelConfidence = 1.0;
        public boolean onnxInferenceExecuted = true;
        public Map<String, Object> featureVector = new LinkedHashMap<>();
    }

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Microsoft ONNX Runtime Environment for AI Shield...");
            env = OrtEnvironment.getEnvironment("AiShieldMlEngine");

            ClassPathResource modelResource = new ClassPathResource("models/fraud_model.onnx");
            if (modelResource.exists()) {
                byte[] modelBytes = modelResource.getInputStream().readAllBytes();
                session = env.createSession(modelBytes, new OrtSession.SessionOptions());
                modelLoaded = true;
                log.info("Successfully loaded 'fraud_model.onnx' into ONNX Runtime in-process session.");
            } else {
                log.warn("ONNX model 'models/fraud_model.onnx' not found on classpath. Operating in fallback mode.");
            }

            // Load model metadata if available
            ClassPathResource metaResource = new ClassPathResource("models/model_metadata.json");
            if (metaResource.exists()) {
                try (InputStream is = metaResource.getInputStream()) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(is);
                    if (node.has("model_name")) modelVersion = node.get("model_name").asText();
                    if (node.has("algorithm")) algorithm = node.get("algorithm").asText();
                    if (node.has("metrics") && node.get("metrics").has("pr_auc")) {
                        prAuc = node.get("metrics").get("pr_auc").asDouble();
                        rocAuc = node.get("metrics").get("roc_auc").asDouble();
                        modelConfidence = prAuc;
                    }
                    log.info("Loaded ML Model Metadata: {} | Algorithm: {} | PR-AUC: {}", modelVersion, algorithm, prAuc);
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize ONNX Runtime session: {}. Operating in safe fallback mode.", e.getMessage(), e);
            modelLoaded = false;
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
            log.info("ONNX Runtime resources released cleanly.");
        } catch (Exception e) {
            log.warn("Error releasing ONNX resources: {}", e.getMessage());
        }
    }

    public MLScoringResult computeMLScore(Double amount, String location, String merchantCategory, 
                                          LocalDateTime timestamp, int historySize, double ruleScore,
                                          RuleEvaluator.RuleEvaluationResult ruleResult,
                                          List<TransactionEntity> previousTransactions) {
        MLScoringResult result = new MLScoringResult();
        result.modelVersion = this.modelVersion;
        result.algorithm = this.algorithm;
        result.modelConfidence = this.modelConfidence;

        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }

        // ==========================================
        // 1. EXTRACT 10-DIMENSIONAL FEATURE VECTOR
        // ==========================================
        // f1: amount_norm
        float amountNorm = (amount != null) ? (float) Math.min(1.0, amount / 250000.0) : 0.05f;

        // f2: amount_ratio (vs baseline)
        float amountRatio = 1.0f;
        if (previousTransactions != null && !previousTransactions.isEmpty() && amount != null) {
            double avg = previousTransactions.stream()
                    .mapToDouble(TransactionEntity::getAmount)
                    .average()
                    .orElse(amount);
            if (avg > 0) {
                amountRatio = (float) Math.min(15.0, amount / avg);
            }
        }

        // f3: time_entropy (midnight spending)
        int hour = timestamp.getHour();
        float timeEntropy = (hour >= 1 && hour <= 5) ? 0.85f : 0.15f;

        // f4: merchant_risk
        float merchantRisk = 0.10f;
        if (merchantCategory != null) {
            String upper = merchantCategory.toUpperCase();
            if (upper.contains("CRYPTO") || upper.contains("GAMBLING") || upper.contains("CASINO")) {
                merchantRisk = 0.95f;
            } else if (upper.contains("TRAVEL") || upper.contains("JEWELRY") || upper.contains("ELECTRONICS")) {
                merchantRisk = 0.45f;
            }
        }

        // f5 & f6: velocity_10m & velocity_1h
        float velocity10m = 0.0f;
        float velocity1h = 0.0f;
        if (previousTransactions != null && !previousTransactions.isEmpty()) {
            LocalDateTime tenMinAgo = timestamp.minusMinutes(10);
            LocalDateTime oneHourAgo = timestamp.minusHours(1);
            velocity10m = (float) previousTransactions.stream()
                    .filter(t -> t.getTimestamp() != null && t.getTimestamp().isAfter(tenMinAgo))
                    .count();
            velocity1h = (float) previousTransactions.stream()
                    .filter(t -> t.getTimestamp() != null && t.getTimestamp().isAfter(oneHourAgo))
                    .count();
        }

        // f7: impossible_travel
        float impossibleTravel = (ruleResult != null && ruleResult.impossibleTravelTriggered) ? 1.0f : 0.0f;

        // f8: cross_city
        float crossCity = 0.0f;
        if (previousTransactions != null && !previousTransactions.isEmpty() && location != null) {
            TransactionEntity lastTx = previousTransactions.get(0);
            if (lastTx.getLocation() != null && !lastTx.getLocation().equalsIgnoreCase(location)) {
                crossCity = 1.0f;
            }
        }

        // f9: cold_start
        float coldStart = (historySize <= 1) ? 1.0f : 0.0f;

        // f10: rule_score_norm
        float ruleScoreNorm = (float) Math.min(1.0, Math.max(0.0, ruleScore / 100.0));

        // Save structured feature map for explainability
        result.featureVector.put("amountNorm", round(amountNorm, 3));
        result.featureVector.put("amountRatio", round(amountRatio, 2));
        result.featureVector.put("timeEntropy", round(timeEntropy, 2));
        result.featureVector.put("merchantRisk", round(merchantRisk, 2));
        result.featureVector.put("velocity10m", (int) velocity10m);
        result.featureVector.put("velocity1h", (int) velocity1h);
        result.featureVector.put("impossibleTravel", (int) impossibleTravel);
        result.featureVector.put("crossCity", (int) crossCity);
        result.featureVector.put("coldStart", (int) coldStart);
        result.featureVector.put("ruleScoreNorm", round(ruleScoreNorm, 2));

        // ==========================================
        // 2. REAL ONNX TENSOR INFERENCE
        // ==========================================
        if (modelLoaded && session != null && env != null) {
            try {
                float[][] inputData = new float[1][10];
                inputData[0][0] = amountNorm;
                inputData[0][1] = amountRatio;
                inputData[0][2] = timeEntropy;
                inputData[0][3] = merchantRisk;
                inputData[0][4] = velocity10m;
                inputData[0][5] = velocity1h;
                inputData[0][6] = impossibleTravel;
                inputData[0][7] = crossCity;
                inputData[0][8] = coldStart;
                inputData[0][9] = ruleScoreNorm;

                try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData)) {
                    Map<String, OnnxTensor> inputMap = Collections.singletonMap("float_input", inputTensor);
                    try (OrtSession.Result ortResult = session.run(inputMap)) {
                        if (ortResult.size() > 1) {
                            OnnxValue probValue = ortResult.get(1);
                            Object probObj = probValue.getValue();
                            if (probObj instanceof float[][]) {
                                float[][] probs = (float[][]) probObj;
                                double pFraud = probs[0][1];
                                result.mlProbability = round(pFraud, 4);
                                result.mlScore = round(pFraud * 100.0, 1);
                                result.onnxInferenceExecuted = true;
                                return result;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("ONNX tensor execution encountered exception: {}. Using fallback scoring.", e.getMessage());
            }
        }

        // ==========================================
        // 3. ROBUST MATHEMATICAL BACKUP (If ONNX fails)
        // ==========================================
        result.onnxInferenceExecuted = false;
        double logit = (0.40 * amountNorm) + (0.25 * merchantRisk) + (0.15 * timeEntropy) 
                     + (0.35 * (impossibleTravel)) + (0.25 * ruleScoreNorm) + (0.10 * coldStart);
        double probability = 1.0 / (1.0 + Math.exp(-6.0 * (logit - 0.5)));
        result.mlProbability = round(Math.max(0.01, Math.min(0.99, probability)), 4);
        result.mlScore = round(result.mlProbability * 100.0, 1);

        return result;
    }

    public MLScoringResult computeMLScore(Double amount, String location, String merchantCategory, 
                                          LocalDateTime timestamp, int historySize, double ruleScore) {
        return computeMLScore(amount, location, merchantCategory, timestamp, historySize, ruleScore, null, null);
    }

    private static double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
