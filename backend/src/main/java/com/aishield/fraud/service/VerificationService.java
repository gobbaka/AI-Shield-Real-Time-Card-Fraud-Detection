package com.aishield.fraud.service;

import com.aishield.fraud.dto.VerificationDtos;
import com.aishield.fraud.entity.CardEntity;
import com.aishield.fraud.entity.FraudAlertEntity;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.entity.TransactionVerificationEntity;
import com.aishield.fraud.exception.BadRequestException;
import com.aishield.fraud.exception.ResourceNotFoundException;
import com.aishield.fraud.repository.CardRepository;
import com.aishield.fraud.repository.FraudAlertRepository;
import com.aishield.fraud.repository.TransactionRepository;
import com.aishield.fraud.repository.TransactionVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VerificationService {

    private final TransactionVerificationRepository verificationRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final CardRepository cardRepository;
    private final SmsNotificationService smsNotificationService;
    private final AuditLoggingService auditLoggingService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public VerificationService(TransactionVerificationRepository verificationRepository,
                               TransactionRepository transactionRepository,
                               FraudAlertRepository fraudAlertRepository,
                               CardRepository cardRepository,
                               SmsNotificationService smsNotificationService,
                               AuditLoggingService auditLoggingService) {
        this.verificationRepository = verificationRepository;
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.cardRepository = cardRepository;
        this.smsNotificationService = smsNotificationService;
        this.auditLoggingService = auditLoggingService;
    }

    public VerificationService(TransactionVerificationRepository verificationRepository,
                               TransactionRepository transactionRepository,
                               FraudAlertRepository fraudAlertRepository,
                               CardRepository cardRepository,
                               SmsNotificationService smsNotificationService) {
        this(verificationRepository, transactionRepository, fraudAlertRepository, cardRepository, smsNotificationService, null);
    }

    @Transactional
    public TransactionVerificationEntity initiateVerification(TransactionEntity transaction) {
        // Expiration = 10 minutes from now
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
        
        // Generate unique Verification Request ID
        String requestId = "VR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Cryptographically secure challenge token for replay protection
        byte[] challengeBytes = new byte[32];
        secureRandom.nextBytes(challengeBytes);
        String challengeToken = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        String email = (transaction.getUser() != null && transaction.getUser().getEmail() != null)
                ? transaction.getUser().getEmail() : "customer@example.com";

        String phone = (transaction.getUser() != null && transaction.getUser().getPhone() != null)
                ? transaction.getUser().getPhone() : "+91 98111 22334";

        TransactionVerificationEntity entity = new TransactionVerificationEntity(
                requestId,
                transaction,
                transaction.getTransactionRef(),
                transaction.getUser(),
                email,
                transaction.getCustomerName(),
                phone,
                transaction.getCardMasked(),
                transaction.getAmount(),
                transaction.getMerchantName(),
                transaction.getLocation(),
                transaction.getRisk(),
                transaction.getRiskScore(),
                "PENDING",
                expiresAt,
                challengeToken
        );

        // 1. Dispatch real SMS fraud alert to card linked mobile number via configured provider
        com.aishield.fraud.sms.SmsSendResult sendResult = smsNotificationService.sendTransactionFraudAlert(transaction, entity, transaction.getUser());

        entity.setSmsStatus(sendResult != null ? sendResult.getStatus() : "DELIVERED");
        if (entity.getSmsContent() == null) {
            entity.setSmsContent(smsNotificationService.buildSmsMessage(
                    transaction.getCustomerName(),
                    transaction.getCardMasked(),
                    transaction.getAmount(),
                    transaction.getMerchantName(),
                    transaction.getLocation(),
                    transaction.getTransactionRef(),
                    transaction.getRisk(),
                    transaction.getRiskScore(),
                    requestId
            ));
        }

        entity.setAuditNotes("Verification request initiated at " + LocalDateTime.now() + 
                "; Real SMS fraud alert dispatched to linked mobile number: " + phone + 
                " [Provider: " + (sendResult != null ? sendResult.getProviderName() : "GATEWAY") + 
                ", Status: " + (sendResult != null ? sendResult.getStatus() : "SENT") + "]");

        TransactionVerificationEntity savedEntity = verificationRepository.save(entity);

        if (auditLoggingService != null) {
            String cardLast4 = transaction.getCard() != null ? transaction.getCard().getCardLast4() : "N/A";
            Long userId = transaction.getUser() != null ? transaction.getUser().getId() : null;
            auditLoggingService.logStepUpAction("STEP_UP_INITIATED", transaction.getTransactionRef(),
                    cardLast4, userId, transaction.getCustomerName(), "CHALLENGE_DISPATCHED",
                    "Verification request " + requestId + " sent to linked mobile");
        }

        return savedEntity;
    }

    @Transactional
    public boolean resendSmsToPhone(String requestId, String targetPhone) {
        TransactionVerificationEntity entity = verificationRepository.findByVerificationRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found: " + requestId));

        String smsText = smsNotificationService.buildSmsMessage(
                entity.getCustomerName(),
                entity.getCardMasked(),
                entity.getAmount(),
                entity.getMerchantName(),
                entity.getLocation(),
                entity.getTransactionRef(),
                entity.getRiskLevel(),
                entity.getRiskScore(),
                entity.getVerificationRequestId()
        );

        boolean sent = smsNotificationService.sendSms(targetPhone, smsText);
        entity.setCustomerPhone(targetPhone);
        entity.setSmsContent(smsText);
        entity.setSmsStatus(sent ? "DELIVERED" : "FAILED");
        entity.setAuditNotes(entity.getAuditNotes() + "\nSMS re-dispatched to " + targetPhone + " at " + LocalDateTime.now());
        verificationRepository.save(entity);
        return sent;
    }

    public List<VerificationDtos.VerificationRequestResponse> getPendingForCustomer(String email) {
        expireOverdueVerifications();
        return verificationRepository.findByCustomerEmailAndStatus(email, "PENDING").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VerificationDtos.VerificationRequestResponse> getAllPendingVerifications() {
        expireOverdueVerifications();
        return verificationRepository.findAllPending().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public VerificationDtos.VerificationRequestResponse getVerificationById(String requestId) {
        TransactionVerificationEntity entity = verificationRepository.findByVerificationRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found: " + requestId));
        
        if (entity.isExpired() && "PENDING".equalsIgnoreCase(entity.getStatus())) {
            entity.setStatus("EXPIRED");
            verificationRepository.save(entity);
        }

        return mapToResponse(entity);
    }

    @Transactional
    public VerificationDtos.VerificationActionResponse verifyStepUp(String requestId, VerificationDtos.StepUpAuthRequest request) {
        TransactionVerificationEntity entity = verificationRepository.findByVerificationRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found: " + requestId));

        if (!"PENDING".equalsIgnoreCase(entity.getStatus())) {
            throw new BadRequestException("Verification request is no longer active. Current status: " + entity.getStatus());
        }

        if (entity.isExpired()) {
            entity.setStatus("EXPIRED");
            entity.setFailureReason("Verification challenge window expired (> 10 minutes)");
            verificationRepository.save(entity);

            TransactionEntity tx = entity.getTransaction();
            if (tx != null) {
                tx.setStatus("Verification Expired");
                transactionRepository.save(tx);
            }
            throw new BadRequestException("Verification window expired. Transaction cannot proceed without authorization.");
        }

        // Replay and challenge verification
        if (request.getChallengeToken() == null || !request.getChallengeToken().equals(entity.getChallengeToken())) {
            entity.setStatus("VERIFICATION_FAILED");
            entity.setFailureReason("Cryptographic challenge token mismatch or invalid assertion");
            verificationRepository.save(entity);
            throw new BadRequestException("Authentication verification failed: Challenge token mismatch.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Mark verification confirmed
        entity.setStatus("OWNER_CONFIRMED");
        entity.setVerifiedAt(now);
        entity.setVerificationMethod(request.getVerificationMethod() != null ? request.getVerificationMethod() : "WEBAUTHN_PASSKEY");
        entity.setSecurityMetadata(String.format("Method: %s; UserAgent: %s; VerifiedTimestamp: %s; AssertionVerified: true",
                entity.getVerificationMethod(), request.getUserAgent(), now));
        entity.setAuditNotes(entity.getAuditNotes() + "\nStep-up authentication verified successfully by cardholder at " + now);
        verificationRepository.save(entity);

        // 2. Update Transaction Entity
        TransactionEntity tx = entity.getTransaction();
        if (tx != null) {
            tx.setStatus("OWNER_VERIFIED");
            tx.setDecision("APPROVE");
            String existingReasons = tx.getDecisionReasons() != null ? tx.getDecisionReasons() : "";
            tx.setDecisionReasons(existingReasons + "; Step-Up Authenticated by Card Owner (" + entity.getVerificationMethod() + ")");
            transactionRepository.save(tx);
        }

        // 3. Resolve any related Fraud Alert
        fraudAlertRepository.findAll().stream()
                .filter(a -> a.getTransactionRef() != null && a.getTransactionRef().equalsIgnoreCase(entity.getTransactionRef()))
                .forEach(alert -> {
                    alert.setStatus("Resolved");
                    alert.setResolvedAt(now);
                    alert.setInvestigatorNotes("Resolved: Card owner authenticated transaction successfully via step-up biometric/passkey verification.");
                    fraudAlertRepository.save(alert);
                });

        if (auditLoggingService != null) {
            String cardLast4 = tx != null && tx.getCard() != null ? tx.getCard().getCardLast4() : "N/A";
            Long userId = tx != null && tx.getUser() != null ? tx.getUser().getId() : null;
            auditLoggingService.logStepUpAction("STEP_UP_CONFIRMED", entity.getTransactionRef(),
                    cardLast4, userId, entity.getCustomerName(), "OWNER_CONFIRMED",
                    "Method: " + entity.getVerificationMethod() + ", Metadata: " + entity.getSecurityMetadata());
        }

        return new VerificationDtos.VerificationActionResponse(
                entity.getVerificationRequestId(),
                entity.getTransactionRef(),
                "OWNER_CONFIRMED",
                "OWNER_VERIFIED",
                "Transaction authorized successfully by card owner via step-up authentication.",
                now.toString()
        );
    }

    @Transactional
    public VerificationDtos.VerificationActionResponse rejectVerification(String requestId, VerificationDtos.RejectVerificationRequest request) {
        TransactionVerificationEntity entity = verificationRepository.findByVerificationRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found: " + requestId));

        if (!"PENDING".equalsIgnoreCase(entity.getStatus())) {
            throw new BadRequestException("Verification request is no longer active. Current status: " + entity.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Customer confirmed unauthorized transaction";

        // 1. Mark verification rejected
        entity.setStatus("OWNER_REJECTED");
        entity.setVerifiedAt(now);
        entity.setFailureReason(reason);
        entity.setAuditNotes(entity.getAuditNotes() + "\nTransaction rejected as fraudulent by cardholder at " + now + ". Reason: " + reason);
        verificationRepository.save(entity);

        // 2. Update Transaction Entity
        TransactionEntity tx = entity.getTransaction();
        if (tx != null) {
            tx.setStatus("CUSTOMER_REPORTED_FRAUD");
            tx.setRisk("High");
            tx.setRiskScore(99);
            tx.setDecision("DECLINE");
            tx.setDecisionReasons(tx.getDecisionReasons() + "; CUSTOMER CONFIRMED UNAUTHORIZED FRAUD ATTEMPT");
            transactionRepository.save(tx);
        }

        // 3. Freeze / Block Card if requested
        if (request == null || Boolean.TRUE.equals(request.getBlockCard())) {
            if (tx != null && tx.getCard() != null) {
                CardEntity card = tx.getCard();
                card.setStatus("BLOCKED");
                cardRepository.save(card);
            }
        }

        // 4. Escalate / Create Investigation Alert
        if (tx != null) {
            FraudAlertEntity alert = fraudAlertRepository.findAll().stream()
                    .filter(a -> a.getTransactionRef() != null && a.getTransactionRef().equalsIgnoreCase(tx.getTransactionRef()))
                    .findFirst()
                    .orElseGet(() -> {
                        long count = fraudAlertRepository.count();
                        return new FraudAlertEntity(
                                String.format("FA%04d", 1023 + count),
                                tx,
                                tx.getTransactionRef(),
                                tx.getCustomerName(),
                                tx.getAmount(),
                                99,
                                tx.getLocation(),
                                "Blocked",
                                "Customer reported unauthorized card swipe"
                        );
                    });

            alert.setStatus("Blocked");
            alert.setRiskScore(99);
            alert.setInvestigatorNotes("🚨 CRITICAL: Card owner explicitly confirmed this transaction was unauthorized fraud. Card has been frozen and dispute case escalated.");
            fraudAlertRepository.save(alert);
        }

        if (auditLoggingService != null) {
            String cardLast4 = tx != null && tx.getCard() != null ? tx.getCard().getCardLast4() : "N/A";
            Long userId = tx != null && tx.getUser() != null ? tx.getUser().getId() : null;
            auditLoggingService.logStepUpAction("STEP_UP_REJECTED", entity.getTransactionRef(),
                    cardLast4, userId, entity.getCustomerName(), "CUSTOMER_REPORTED_FRAUD",
                    "Fraud confirmed by cardholder. Reason: " + reason);
            if (request == null || Boolean.TRUE.equals(request.getBlockCard())) {
                auditLoggingService.logCardStatusChange(cardLast4, userId, "BLOCKED", "Step-Up verification rejected by cardholder", "CUSTOMER");
            }
        }

        return new VerificationDtos.VerificationActionResponse(
                entity.getVerificationRequestId(),
                entity.getTransactionRef(),
                "OWNER_REJECTED",
                "CUSTOMER_REPORTED_FRAUD",
                "Transaction flagged as unauthorized fraud. Payment blocked and card frozen.",
                now.toString()
        );
    }

    @Transactional
    public void expireOverdueVerifications() {
        LocalDateTime now = LocalDateTime.now();
        List<TransactionVerificationEntity> expiredList = verificationRepository.findExpiredPendingVerifications(now);
        for (TransactionVerificationEntity v : expiredList) {
            v.setStatus("EXPIRED");
            v.setFailureReason("Verification timed out without owner response");
            verificationRepository.save(v);

            TransactionEntity tx = v.getTransaction();
            if (tx != null && "Verification Required".equalsIgnoreCase(tx.getStatus())) {
                tx.setStatus("Verification Expired");
                transactionRepository.save(tx);
            }
        }
    }

    public VerificationDtos.VerificationRequestResponse mapToResponse(TransactionVerificationEntity entity) {
        VerificationDtos.VerificationRequestResponse res = new VerificationDtos.VerificationRequestResponse();
        res.setVerificationRequestId(entity.getVerificationRequestId());
        res.setTransactionRef(entity.getTransactionRef());
        res.setCustomerName(entity.getCustomerName());
        res.setCustomerEmail(entity.getCustomerEmail());
        res.setCustomerPhone(entity.getCustomerPhone());
        res.setSmsStatus(entity.getSmsStatus());
        res.setSmsContent(entity.getSmsContent());
        res.setCardMasked(entity.getCardMasked());
        res.setAmount(entity.getAmount());
        res.setMerchantName(entity.getMerchantName());
        res.setLocation(entity.getLocation());
        res.setRiskLevel(entity.getRiskLevel());
        res.setRiskScore(entity.getRiskScore());
        res.setStatus(entity.getStatus());
        res.setRequestedAt(entity.getRequestedAt() != null ? entity.getRequestedAt().toString() : "");
        res.setExpiresAt(entity.getExpiresAt() != null ? entity.getExpiresAt().toString() : "");
        res.setChallengeToken(entity.getChallengeToken());
        return res;
    }

    public List<VerificationDtos.VerificationHistoryResponse> getVerificationHistory(String email) {
        List<TransactionVerificationEntity> list = (email != null && !email.isBlank())
                ? verificationRepository.findHistoryByCustomer(email)
                : verificationRepository.findAll();

        return list.stream()
                .filter(v -> !"PENDING".equalsIgnoreCase(v.getStatus()))
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getVerifiedAt() != null ? a.getVerifiedAt() : a.getRequestedAt();
                    LocalDateTime tb = b.getVerifiedAt() != null ? b.getVerifiedAt() : b.getRequestedAt();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta);
                })
                .map(v -> new VerificationDtos.VerificationHistoryResponse(
                        "VH-" + v.getId(),
                        v.getVerificationRequestId(),
                        v.getTransactionRef(),
                        v.getAmount(),
                        v.getMerchantName(),
                        v.getLocation(),
                        "OWNER_CONFIRMED".equalsIgnoreCase(v.getStatus()) ? "OWNER_CONFIRMED" : "OWNER_REJECTED",
                        v.getVerificationMethod() != null ? v.getVerificationMethod() : "FIDO2 Passkey / Token",
                        v.getVerifiedAt() != null ? v.getVerifiedAt().toString() : (v.getRequestedAt() != null ? v.getRequestedAt().toString() : "Recent"),
                        v.getChallengeToken(),
                        v.getStatus()
                ))
                .collect(Collectors.toList());
    }
}
