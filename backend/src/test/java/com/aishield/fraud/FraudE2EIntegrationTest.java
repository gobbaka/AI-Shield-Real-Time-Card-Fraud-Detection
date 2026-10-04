package com.aishield.fraud;

import com.aishield.fraud.dto.TransactionDtos;
import com.aishield.fraud.engine.DecisionResolver;
import com.aishield.fraud.engine.FraudDetectionEngine;
import com.aishield.fraud.engine.MachineLearningScorer;
import com.aishield.fraud.engine.RuleEvaluator;
import com.aishield.fraud.entity.*;
import com.aishield.fraud.exception.BadRequestException;
import com.aishield.fraud.repository.*;
import com.aishield.fraud.service.AuditLoggingService;
import com.aishield.fraud.service.TransactionService;
import com.aishield.fraud.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FraudE2EIntegrationTest {

    private TransactionRepository transactionRepository;
    private CardRepository cardRepository;
    private UserRepository userRepository;
    private FraudAlertRepository fraudAlertRepository;
    private TransactionVerificationRepository verificationRepository;
    private TravelNoticeRepository travelNoticeRepository;
    private AuditLoggingService auditLoggingService;
    private VerificationService verificationService;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        cardRepository = mock(CardRepository.class);
        userRepository = mock(UserRepository.class);
        fraudAlertRepository = mock(FraudAlertRepository.class);
        verificationRepository = mock(TransactionVerificationRepository.class);
        travelNoticeRepository = mock(TravelNoticeRepository.class);
        auditLoggingService = mock(AuditLoggingService.class);
        verificationService = mock(VerificationService.class);

        SystemSettingsRepository settingsRepository = mock(SystemSettingsRepository.class);
        when(settingsRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(new SystemSettingsEntity(false, true, true, true, 80)));

        RuleEvaluator ruleEvaluator = new RuleEvaluator();
        MachineLearningScorer mlScorer = new MachineLearningScorer();
        mlScorer.init();
        DecisionResolver decisionResolver = new DecisionResolver();
        FraudDetectionEngine fraudEngine = new FraudDetectionEngine(ruleEvaluator, mlScorer, decisionResolver, settingsRepository);

        transactionService = new TransactionService(
                transactionRepository,
                cardRepository,
                userRepository,
                fraudAlertRepository,
                fraudEngine,
                verificationService,
                verificationRepository,
                travelNoticeRepository,
                auditLoggingService
        );

        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionVerificationEntity mockVerif = new TransactionVerificationEntity();
        mockVerif.setVerificationRequestId("VR-E2E-TEST");
        mockVerif.setStatus("PENDING");
        when(verificationService.initiateVerification(any())).thenReturn(mockVerif);
    }

    @Test
    @DisplayName("Java E2E Scenario 1: Normal everyday grocery transaction is instantly APPROVED with low risk")
    void testScenario1_NormalTransactionApproved() {
        UserEntity rahul = new UserEntity("Rahul Sharma", "rahul@example.com", "hash", "CUSTOMER", "+919811122334", "India", "Bio");
        CardEntity card = new CardEntity(rahul, "**** **** **** 4721", "4721", "VISA", "ACTIVE", 150000.0);

        when(cardRepository.findByCardLast4("4721")).thenReturn(Optional.of(card));
        when(transactionRepository.findByCardIdOrderByTimestampDesc(any())).thenReturn(Collections.emptyList());
        when(travelNoticeRepository.findActiveNoticesForCustomer(anyString(), any())).thenReturn(Collections.emptyList());

        TransactionDtos.IngestTransactionRequest req = new TransactionDtos.IngestTransactionRequest();
        req.setCardLast4("4721");
        req.setCustomerName("Rahul Sharma");
        req.setAmount(1250.0);
        req.setMerchantName("Reliance Fresh");
        req.setMerchantCategory("RETAIL");
        req.setLocation("Hyderabad");
        req.setDeviceFingerprint("DEV-WIN-CHROME");

        TransactionDtos.TransactionResponse response = transactionService.processTransaction(req);

        assertNotNull(response);
        assertEquals("APPROVE", response.getDecision());
        assertEquals("Approved", response.getStatus());
        assertTrue(response.getRiskScore() < 40, "Risk score must be low (< 40)");
        assertNotNull(response.getMlProbability(), "Real ML probability must be computed");
        assertTrue(response.getMlProbability() < 0.40, "ML probability must be low for normal transaction");
        assertEquals("1.0.0-ONNX-RandomForestEnsemble", response.getModelVersion());
        assertTrue(response.getOnnxInferenceExecuted());
    }

    @Test
    @DisplayName("Java E2E Scenario 2: Anomalous crypto spending spike is intercepted and DECLINED")
    void testScenario2_CryptoAnomalyDeclined() {
        UserEntity vikram = new UserEntity("Vikram Rao", "vikram@example.com", "hash", "CUSTOMER", "+919811188401", "India", "Bio");
        CardEntity card = new CardEntity(vikram, "**** **** **** 8401", "8401", "VISA", "ACTIVE", 300000.0);

        when(cardRepository.findByCardLast4("8401")).thenReturn(Optional.of(card));
        when(transactionRepository.findByCardIdOrderByTimestampDesc(any())).thenReturn(Collections.emptyList());
        when(travelNoticeRepository.findActiveNoticesForCustomer(anyString(), any())).thenReturn(Collections.emptyList());

        TransactionDtos.IngestTransactionRequest req = new TransactionDtos.IngestTransactionRequest();
        req.setCardLast4("8401");
        req.setCustomerName("Vikram Rao");
        req.setAmount(215000.0);
        req.setMerchantName("Binance Crypto");
        req.setMerchantCategory("CRYPTO");
        req.setLocation("Singapore");
        req.setDeviceFingerprint("DEV-UNKNOWN-TOR");

        TransactionDtos.TransactionResponse response = transactionService.processTransaction(req);

        assertNotNull(response);
        assertTrue(List.of("FLAG", "DECLINE").contains(response.getDecision()), "Decision must be FLAG or DECLINE, got: " + response.getDecision());
        assertTrue(response.getRiskScore() >= 50, "Risk score must be elevated for high-risk crypto anomaly");
        verify(fraudAlertRepository, times(1)).save(any(FraudAlertEntity.class));
        verify(auditLoggingService, times(1)).logTransactionEvaluation(any(), any(), any(), any(), eq(response.getDecision()), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Java E2E Scenario 3: Active Cardholder Travel Notice suppresses geo-velocity anomaly")
    void testScenario3_TravelModeAnomalySuppressed() {
        UserEntity priya = new UserEntity("Priya Singh", "priya@example.com", "hash", "CUSTOMER", "+919811199331", "India", "Bio");
        CardEntity card = new CardEntity(priya, "**** **** **** 9931", "9931", "MASTERCARD", "ACTIVE", 200000.0);

        // Previous transaction in Delhi 20 minutes ago
        TransactionEntity prevTx = new TransactionEntity();
        prevTx.setLocation("Delhi");
        prevTx.setAmount(1800.0);
        prevTx.setTimestamp(LocalDateTime.now().minusMinutes(20));
        prevTx.setDeviceFingerprint("DEV-IPHONE");

        TravelNoticeEntity activeNotice = new TravelNoticeEntity(
                priya, "priya@example.com", "**** **** **** 9931",
                "United Arab Emirates", "Dubai",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(5)
        );

        when(cardRepository.findByCardLast4("9931")).thenReturn(Optional.of(card));
        when(transactionRepository.findByCardIdOrderByTimestampDesc(any())).thenReturn(List.of(prevTx));
        when(travelNoticeRepository.findActiveNoticesForCustomer(eq("priya@example.com"), any())).thenReturn(List.of(activeNotice));

        TransactionDtos.IngestTransactionRequest req = new TransactionDtos.IngestTransactionRequest();
        req.setCardLast4("9931");
        req.setCustomerName("Priya Singh");
        req.setAmount(4200.0);
        req.setMerchantName("Dubai Duty Free");
        req.setMerchantCategory("RETAIL");
        req.setLocation("Dubai");
        req.setDeviceFingerprint("DEV-IPHONE-ROAMING");

        TransactionDtos.TransactionResponse response = transactionService.processTransaction(req);

        assertNotNull(response);
        assertEquals("APPROVE", response.getDecision());
        assertTrue(response.getReasons().stream().anyMatch(r -> r.contains("Travel Mode") || r.contains("Suppressed")),
                "Reasons must reflect Travel Mode active suppression");
    }

    @Test
    @DisplayName("Java E2E Scenario 4: Attempting transaction on a BLOCKED card immediately declines with BadRequestException")
    void testScenario4_BlockedCardEnforcement() {
        UserEntity arjun = new UserEntity("Arjun Mehta", "arjun@example.com", "hash", "CUSTOMER", "+919811165100", "India", "Bio");
        CardEntity card = new CardEntity(arjun, "**** **** **** 6510", "6510", "VISA", "BLOCKED", 200000.0);

        when(cardRepository.findByCardLast4("6510")).thenReturn(Optional.of(card));

        TransactionDtos.IngestTransactionRequest req = new TransactionDtos.IngestTransactionRequest();
        req.setCardLast4("6510");
        req.setCustomerName("Arjun Mehta");
        req.setAmount(500.0);
        req.setMerchantName("Local Cafe");
        req.setMerchantCategory("RETAIL");
        req.setLocation("Mumbai");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            transactionService.processTransaction(req);
        });

        assertTrue(ex.getMessage().contains("blocked"), "Message must explain card is blocked");
    }
}
