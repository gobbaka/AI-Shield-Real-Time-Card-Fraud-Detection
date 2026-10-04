package com.aishield.fraud.service;

import com.aishield.fraud.dto.TransactionDtos;
import com.aishield.fraud.engine.FraudAnalysisResult;
import com.aishield.fraud.engine.FraudDetectionEngine;
import com.aishield.fraud.entity.CardEntity;
import com.aishield.fraud.entity.FraudAlertEntity;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.entity.TransactionVerificationEntity;
import com.aishield.fraud.entity.UserEntity;
import com.aishield.fraud.exception.BadRequestException;
import com.aishield.fraud.exception.ResourceNotFoundException;
import com.aishield.fraud.repository.CardRepository;
import com.aishield.fraud.repository.FraudAlertRepository;
import com.aishield.fraud.repository.TransactionRepository;
import com.aishield.fraud.repository.TransactionVerificationRepository;
import com.aishield.fraud.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudDetectionEngine fraudEngine;
    private final VerificationService verificationService;
    private final TransactionVerificationRepository verificationRepository;
    private final com.aishield.fraud.repository.TravelNoticeRepository travelNoticeRepository;
    private final AuditLoggingService auditLoggingService;

    public TransactionService(TransactionRepository transactionRepository,
                              CardRepository cardRepository,
                              UserRepository userRepository,
                              FraudAlertRepository fraudAlertRepository,
                              FraudDetectionEngine fraudEngine,
                              @Lazy VerificationService verificationService,
                              TransactionVerificationRepository verificationRepository,
                              com.aishield.fraud.repository.TravelNoticeRepository travelNoticeRepository,
                              AuditLoggingService auditLoggingService) {
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudEngine = fraudEngine;
        this.verificationService = verificationService;
        this.verificationRepository = verificationRepository;
        this.travelNoticeRepository = travelNoticeRepository;
        this.auditLoggingService = auditLoggingService;
    }

    @Transactional
    public TransactionDtos.TransactionResponse processTransaction(TransactionDtos.IngestTransactionRequest request) {
        // 1. Validate Card and User
        CardEntity card = null;
        UserEntity user = null;

        if (request.getCardLast4() != null && !request.getCardLast4().isBlank()) {
            String last4 = request.getCardLast4().replaceAll("[^0-9]", "");
            if (last4.length() > 4) {
                last4 = last4.substring(last4.length() - 4);
            }
            card = cardRepository.findByCardLast4(last4).orElse(null);
        }

        if (card != null) {
            user = card.getUser();
        } else if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        } else if (request.getCustomerName() != null) {
            user = userRepository.findAll().stream()
                    .filter(u -> u.getName().equalsIgnoreCase(request.getCustomerName()))
                    .findFirst()
                    .orElse(null);
        }

        // Check if card is blocked
        if (card != null && "BLOCKED".equalsIgnoreCase(card.getStatus())) {
            throw new BadRequestException("Transaction declined: The card is currently blocked.");
        }

        String customerName = (user != null) ? user.getName() : 
                (request.getCustomerName() != null ? request.getCustomerName() : "Cardholder");
        String maskedCard = (card != null) ? card.getCardNumberMasked() : 
                (request.getCardLast4() != null ? "**** **** **** " + request.getCardLast4() : "**** **** **** 8888");

        // 2. Fetch Historical Transactions for Behavioral Baseline (Cold-Start safe)
        List<TransactionEntity> history = new ArrayList<>();
        if (card != null) {
            history = transactionRepository.findByCardIdOrderByTimestampDesc(card.getId());
        } else if (user != null) {
            history = transactionRepository.findByUserIdOrderByTimestampDesc(user.getId());
        }

        // Fetch active Travel Notices for cardholder
        List<com.aishield.fraud.entity.TravelNoticeEntity> activeNotices = (user != null)
                ? travelNoticeRepository.findActiveNoticesForCustomer(user.getEmail(), java.time.LocalDate.now())
                : java.util.Collections.emptyList();

        // 3. Execute Fraud Detection Engine (Rule + Real ML scoring)
        LocalDateTime now = LocalDateTime.now();
        FraudAnalysisResult analysis = fraudEngine.analyze(
                request.getAmount(),
                request.getLocation(),
                request.getMerchantCategory(),
                now,
                request.getDeviceFingerprint(),
                history,
                activeNotices
        );

        // 4. Generate unique transaction reference
        long txCount = transactionRepository.count();
        String txRef = String.format("#TX%05d", 10001 + txCount);

        // Determine if step-up verification is required (for suspicious/high-risk transactions)
        String initialStatus = analysis.getTransactionStatus();
        if (analysis.isShouldGenerateAlert() || analysis.getRiskScore() >= 40) {
            initialStatus = "Verification Required";
        }

        // 5. Persist Transaction (D2)
        String reasonsJoined = String.join("; ", analysis.getReasons());
        TransactionEntity entity = new TransactionEntity(
                txRef,
                user,
                card,
                customerName,
                maskedCard,
                request.getAmount(),
                request.getMerchantName() != null ? request.getMerchantName() : "Merchant Outlet",
                request.getMerchantCategory() != null ? request.getMerchantCategory() : "RETAIL",
                request.getLocation(),
                request.getDeviceFingerprint(),
                request.getIpAddress(),
                now,
                initialStatus,
                analysis.getRiskLevel(),
                analysis.getRiskScore(),
                reasonsJoined,
                analysis.getDecision()
        );

        Double mlProb = analysis.getSignals() != null && analysis.getSignals().containsKey("mlProbability")
                ? ((Number) analysis.getSignals().get("mlProbability")).doubleValue() : null;
        Double rScore = analysis.getSignals() != null && analysis.getSignals().containsKey("ruleScore")
                ? ((Number) analysis.getSignals().get("ruleScore")).doubleValue() : null;
        Double mScore = analysis.getSignals() != null && analysis.getSignals().containsKey("mlScore")
                ? ((Number) analysis.getSignals().get("mlScore")).doubleValue() : null;

        entity.setMlProbability(mlProb);
        entity.setRuleScore(rScore);
        entity.setMlScore(mScore);

        TransactionEntity savedTx = transactionRepository.save(entity);

        // Record immutable audit log asynchronously (non-blocking)
        String cardLast4 = card != null ? card.getCardLast4() : 
                (request.getCardLast4() != null ? request.getCardLast4() : "N/A");
        auditLoggingService.logTransactionEvaluation(
                savedTx.getTransactionRef(),
                cardLast4,
                user != null ? user.getId() : null,
                savedTx.getCustomerName(),
                savedTx.getDecision(),
                savedTx.getRiskScore(),
                mlProb,
                reasonsJoined,
                request.getIpAddress(),
                request.getDeviceFingerprint(),
                "Amount: " + savedTx.getAmount() + ", Location: " + savedTx.getLocation() + ", Status: " + initialStatus
        );

        // 6. Generate Alert & Investigation Case (D4) if suspicious/high risk
        if (analysis.isShouldGenerateAlert()) {
            long alertCount = fraudAlertRepository.count();
            String alertRef = String.format("FA%04d", 1023 + alertCount);

            FraudAlertEntity alert = new FraudAlertEntity(
                    alertRef,
                    savedTx,
                    savedTx.getTransactionRef(),
                    savedTx.getCustomerName(),
                    savedTx.getAmount(),
                    savedTx.getRiskScore(),
                    savedTx.getLocation(),
                    "Reviewing",
                    reasonsJoined + " [Step-Up Verification Pending]"
            );
            fraudAlertRepository.save(alert);
        }

        // 7. Initiate Card Owner Step-Up Verification Request if required
        if ("Verification Required".equalsIgnoreCase(initialStatus)) {
            TransactionVerificationEntity verif = verificationService != null ? verificationService.initiateVerification(savedTx) : null;
            TransactionDtos.TransactionResponse response = mapToResponse(savedTx);
            if (verif != null) {
                response.setVerificationRequestId(verif.getVerificationRequestId());
                response.setVerificationStatus(verif.getStatus());
            }
            return response;
        }

        return mapToResponse(savedTx);
    }

    public List<TransactionDtos.TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionDtos.TransactionResponse> getRecentTransactions(int limit) {
        return transactionRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp")))
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TransactionDtos.TransactionResponse getTransactionById(Long id) {
        TransactionEntity entity = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return mapToResponse(entity);
    }

    public TransactionDtos.TransactionResponse getTransactionByRef(String ref) {
        TransactionEntity entity = transactionRepository.findByTransactionRef(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ref: " + ref));
        return mapToResponse(entity);
    }

    @Transactional
    public TransactionDtos.TransactionResponse blockTransaction(String ref) {
        TransactionEntity entity = transactionRepository.findByTransactionRef(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ref: " + ref));

        entity.setStatus("Blocked");
        entity.setRisk("High");
        entity.setDecision("DECLINE");
        TransactionEntity updated = transactionRepository.save(entity);

        // Update corresponding alert if present
        fraudAlertRepository.findAll().stream()
                .filter(a -> ref.equalsIgnoreCase(a.getTransactionRef()))
                .forEach(a -> {
                    a.setStatus("Blocked");
                    fraudAlertRepository.save(a);
                });

        return mapToResponse(updated);
    }

    public TransactionDtos.TransactionResponse mapToResponse(TransactionEntity entity) {
        TransactionDtos.TransactionResponse res = new TransactionDtos.TransactionResponse();
        res.setId(entity.getTransactionRef());
        res.setCustomer(entity.getCustomerName());
        res.setCard(entity.getCardMasked());
        res.setAmount(entity.getAmount());
        res.setLocation(entity.getLocation());
        res.setTimestamp(entity.getTimestamp() != null ? entity.getTimestamp().toString() : "");
        res.setRisk(entity.getRisk());
        res.setRiskScore(entity.getRiskScore());
        res.setStatus(entity.getStatus());
        res.setMerchantName(entity.getMerchantName());
        res.setMerchantCategory(entity.getMerchantCategory());
        res.setDecision(entity.getDecision());

        if (entity.getDecisionReasons() != null && !entity.getDecisionReasons().isBlank()) {
            res.setReasons(List.of(entity.getDecisionReasons().split("; ")));
        } else {
            res.setReasons(List.of("Legitimate transaction attributes verified"));
        }

        // Attach verification info if exists
        verificationRepository.findByTransactionRef(entity.getTransactionRef()).ifPresent(v -> {
            res.setVerificationRequestId(v.getVerificationRequestId());
            res.setVerificationStatus(v.getStatus());
        });

        res.setMlProbability(entity.getMlProbability());
        res.setRuleScore(entity.getRuleScore());
        res.setMlScore(entity.getMlScore());
        res.setModelVersion("1.0.0-ONNX-RandomForestEnsemble");
        res.setOnnxInferenceExecuted(entity.getMlProbability() != null);

        return res;
    }
}
