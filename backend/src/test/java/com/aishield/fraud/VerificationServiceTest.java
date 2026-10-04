package com.aishield.fraud;

import com.aishield.fraud.dto.VerificationDtos;
import com.aishield.fraud.entity.CardEntity;
import com.aishield.fraud.entity.FraudAlertEntity;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.entity.TransactionVerificationEntity;
import com.aishield.fraud.entity.UserEntity;
import com.aishield.fraud.exception.BadRequestException;
import com.aishield.fraud.repository.CardRepository;
import com.aishield.fraud.repository.FraudAlertRepository;
import com.aishield.fraud.repository.TransactionRepository;
import com.aishield.fraud.repository.TransactionVerificationRepository;
import com.aishield.fraud.service.SmsNotificationService;
import com.aishield.fraud.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VerificationServiceTest {

    private TransactionVerificationRepository verificationRepository;
    private TransactionRepository transactionRepository;
    private FraudAlertRepository fraudAlertRepository;
    private CardRepository cardRepository;
    private SmsNotificationService smsNotificationService;
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationRepository = Mockito.mock(TransactionVerificationRepository.class);
        transactionRepository = Mockito.mock(TransactionRepository.class);
        fraudAlertRepository = Mockito.mock(FraudAlertRepository.class);
        cardRepository = Mockito.mock(CardRepository.class);
        smsNotificationService = Mockito.mock(SmsNotificationService.class);

        when(smsNotificationService.sendTransactionFraudAlert(any(), any(), any()))
                .thenReturn(new com.aishield.fraud.sms.SmsSendResult(true, "DELIVERED", "FAST2SMS", "REQ-12345", null));
        when(smsNotificationService.buildSmsMessage(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("AI Shield Security Alert: A transaction of ₹120000 was detected on your card ending in 4721. Verify: http://localhost:4200/verify/VR-123");

        verificationService = new VerificationService(
                verificationRepository, transactionRepository, fraudAlertRepository, cardRepository, smsNotificationService
        );
    }

    @Test
    @DisplayName("Initiating verification creates a secure pending request with a 10-min challenge token and sends SMS")
    void testInitiateVerification() {
        UserEntity user = new UserEntity("Rahul", "rahul@example.com", "hash", "CUSTOMER", "+91 98111 22334", null, null);
        TransactionEntity tx = new TransactionEntity(
                "#TX10099", user, null, "Rahul Sharma", "**** 4721", 120000.0,
                "Merchant", "CRYPTO", "London", "DEV-TOR", "127.0.0.1", LocalDateTime.now(),
                "Verification Required", "High", 95, "Risk Spike", "FLAG"
        );

        when(verificationRepository.save(any(TransactionVerificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionVerificationEntity result = verificationService.initiateVerification(tx);

        assertNotNull(result);
        assertTrue(result.getVerificationRequestId().startsWith("VR-"));
        assertEquals("PENDING", result.getStatus());
        assertEquals("+91 98111 22334", result.getCustomerPhone());
        assertNotNull(result.getChallengeToken());
        assertTrue(result.getChallengeToken().length() >= 32);
        assertTrue(result.getExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals("DELIVERED", result.getSmsStatus());
        assertNotNull(result.getSmsContent());
        assertTrue(result.getSmsContent().contains("₹120000"));
    }

    @Test
    @DisplayName("Valid step-up authentication challenge confirms request and updates transaction to OWNER_VERIFIED")
    void testVerifyStepUpSuccess() {
        TransactionEntity tx = new TransactionEntity();
        tx.setTransactionRef("#TX10099");
        tx.setStatus("Verification Required");

        TransactionVerificationEntity entity = new TransactionVerificationEntity(
                "VR-TEST-123", tx, "#TX10099", null, "rahul@example.com", "Rahul", "+91 98111 22334",
                "**** 4721", 120000.0, "Crypto Ex", "London", "High", 95,
                "PENDING", LocalDateTime.now().plusMinutes(10), "CHALLENGE-TOKEN-VALID"
        );

        when(verificationRepository.findByVerificationRequestId("VR-TEST-123")).thenReturn(Optional.of(entity));
        when(fraudAlertRepository.findAll()).thenReturn(new ArrayList<>());

        VerificationDtos.StepUpAuthRequest req = new VerificationDtos.StepUpAuthRequest();
        req.setChallengeToken("CHALLENGE-TOKEN-VALID");
        req.setVerificationMethod("WEBAUTHN_PASSKEY");
        req.setUserAgent("Mozilla/5.0");

        VerificationDtos.VerificationActionResponse response = verificationService.verifyStepUp("VR-TEST-123", req);

        assertNotNull(response);
        assertEquals("OWNER_CONFIRMED", response.getVerificationStatus());
        assertEquals("OWNER_VERIFIED", response.getTransactionStatus());
        assertEquals("OWNER_VERIFIED", tx.getStatus());
        assertEquals("APPROVE", tx.getDecision());
        verify(verificationRepository, times(1)).save(entity);
        verify(transactionRepository, times(1)).save(tx);
    }

    @Test
    @DisplayName("Owner rejecting transaction marks it CUSTOMER_REPORTED_FRAUD and blocks card")
    void testRejectVerification() {
        CardEntity card = new CardEntity();
        card.setStatus("ACTIVE");

        TransactionEntity tx = new TransactionEntity();
        tx.setTransactionRef("#TX10099");
        tx.setCard(card);
        tx.setStatus("Verification Required");

        TransactionVerificationEntity entity = new TransactionVerificationEntity(
                "VR-TEST-456", tx, "#TX10099", null, "rahul@example.com", "Rahul", "+91 98111 22334",
                "**** 4721", 120000.0, "Crypto Ex", "London", "High", 95,
                "PENDING", LocalDateTime.now().plusMinutes(10), "CHALLENGE-TOKEN-XYZ"
        );

        when(verificationRepository.findByVerificationRequestId("VR-TEST-456")).thenReturn(Optional.of(entity));
        when(fraudAlertRepository.findAll()).thenReturn(new ArrayList<>());

        VerificationDtos.RejectVerificationRequest req = new VerificationDtos.RejectVerificationRequest("Unauthorized swipe", true);
        VerificationDtos.VerificationActionResponse response = verificationService.rejectVerification("VR-TEST-456", req);

        assertNotNull(response);
        assertEquals("OWNER_REJECTED", response.getVerificationStatus());
        assertEquals("CUSTOMER_REPORTED_FRAUD", response.getTransactionStatus());
        assertEquals("CUSTOMER_REPORTED_FRAUD", tx.getStatus());
        assertEquals("BLOCKED", card.getStatus());
        verify(cardRepository, times(1)).save(card);
        verify(transactionRepository, times(1)).save(tx);
    }

    @Test
    @DisplayName("Expired verification request throws BadRequestException and transitions to EXPIRED")
    void testExpiredVerificationThrowsError() {
        TransactionVerificationEntity entity = new TransactionVerificationEntity(
                "VR-TEST-EXP", new TransactionEntity(), "#TX10099", null, "rahul@example.com", "Rahul", "+91 98111 22334",
                "**** 4721", 120000.0, "Crypto Ex", "London", "High", 95,
                "PENDING", LocalDateTime.now().minusMinutes(5), "CHALLENGE-TOKEN-OLD"
        );

        when(verificationRepository.findByVerificationRequestId("VR-TEST-EXP")).thenReturn(Optional.of(entity));

        VerificationDtos.StepUpAuthRequest req = new VerificationDtos.StepUpAuthRequest();
        req.setChallengeToken("CHALLENGE-TOKEN-OLD");

        assertThrows(BadRequestException.class, () -> verificationService.verifyStepUp("VR-TEST-EXP", req));
        assertEquals("EXPIRED", entity.getStatus());
    }
}
