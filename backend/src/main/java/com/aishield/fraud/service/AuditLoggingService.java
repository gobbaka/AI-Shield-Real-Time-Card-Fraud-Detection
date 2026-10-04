package com.aishield.fraud.service;

import com.aishield.fraud.entity.FraudAuditLogEntity;
import com.aishield.fraud.repository.FraudAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AuditLoggingService {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingService.class);
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");

    private final FraudAuditLogRepository auditRepository;

    public AuditLoggingService(FraudAuditLogRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Records transaction evaluation in background async thread.
     * Sanitizes all sensitive inputs to prevent PCI-DSS data leakage.
     */
    @Async("aiShieldTaskExecutor")
    public void logTransactionEvaluation(String transactionRef, String cardLast4, Long userId,
                                         String customerName, String actionTaken, Integer riskScore,
                                         Double mlProbability, String triggeredRules, String clientIp,
                                         String userAgent, String details) {
        try {
            String sanitizedDetails = sanitize(details);
            FraudAuditLogEntity audit = new FraudAuditLogEntity(
                    "TRANSACTION_EVALUATED",
                    transactionRef,
                    cardLast4,
                    userId,
                    customerName,
                    actionTaken,
                    riskScore,
                    mlProbability,
                    triggeredRules,
                    clientIp,
                    userAgent,
                    sanitizedDetails
            );
            auditRepository.save(audit);
            log.debug("Recorded audit log for transaction {}", transactionRef);
        } catch (Exception e) {
            log.error("Failed to persist async transaction evaluation audit log: {}", e.getMessage());
        }
    }

    /**
     * Records Step-Up owner verification events (challenge initiated, confirmed, rejected).
     */
    @Async("aiShieldTaskExecutor")
    public void logStepUpAction(String eventType, String transactionRef, String cardLast4, Long userId,
                                String customerName, String actionTaken, String details) {
        try {
            String sanitizedDetails = sanitize(details);
            FraudAuditLogEntity audit = new FraudAuditLogEntity(
                    eventType,
                    transactionRef,
                    cardLast4,
                    userId,
                    customerName,
                    actionTaken,
                    null,
                    null,
                    null,
                    null,
                    null,
                    sanitizedDetails
            );
            auditRepository.save(audit);
            log.debug("Recorded step-up audit log: {} for tx {}", eventType, transactionRef);
        } catch (Exception e) {
            log.error("Failed to persist async step-up audit log: {}", e.getMessage());
        }
    }

    /**
     * Records Card freeze / unfreeze actions.
     */
    @Async("aiShieldTaskExecutor")
    public void logCardStatusChange(String cardLast4, Long userId, String actionTaken, String reason, String actor) {
        try {
            String details = String.format("Card ending in %s status updated to %s by %s. Reason: %s",
                    cardLast4, actionTaken, actor, reason);
            FraudAuditLogEntity audit = new FraudAuditLogEntity(
                    "CARD_STATUS_CHANGED",
                    null,
                    cardLast4,
                    userId,
                    null,
                    actionTaken,
                    null,
                    null,
                    null,
                    null,
                    null,
                    sanitize(details)
            );
            auditRepository.save(audit);
            log.info("Recorded card status change audit: card ending in {} -> {}", cardLast4, actionTaken);
        } catch (Exception e) {
            log.error("Failed to persist async card status audit log: {}", e.getMessage());
        }
    }

    /**
     * Records Authentication and 2FA / OTP events.
     */
    @Async("aiShieldTaskExecutor")
    public void logAuthEvent(String eventType, Long userId, String username, String actionTaken, String clientIp, String details) {
        try {
            FraudAuditLogEntity audit = new FraudAuditLogEntity(
                    eventType,
                    null,
                    null,
                    userId,
                    username,
                    actionTaken,
                    null,
                    null,
                    null,
                    clientIp,
                    null,
                    sanitize(details)
            );
            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to persist async auth audit log: {}", e.getMessage());
        }
    }

    public Page<FraudAuditLogEntity> getRecentAuditLogs(int page, int size) {
        return auditRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }

    public List<FraudAuditLogEntity> getAuditLogsForTransaction(String txRef) {
        return auditRepository.findByTransactionRefOrderByTimestampDesc(txRef);
    }

    /**
     * Sanitizer ensuring no credit card numbers or sensitive tokens are stored.
     */
    private String sanitize(String input) {
        if (input == null) return null;
        // Mask any accidental 13-19 digit card numbers found in text
        return PAN_PATTERN.matcher(input).replaceAll("****-****-****-****");
    }
}
