package com.aishield.fraud.service;

import com.aishield.fraud.entity.NotificationLogEntity;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.entity.TransactionVerificationEntity;
import com.aishield.fraud.entity.UserEntity;
import com.aishield.fraud.repository.NotificationLogRepository;
import com.aishield.fraud.sms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final Fast2SmsProvider fast2SmsProvider;
    private final TwilioSmsProvider twilioSmsProvider;
    private final MockSmsProvider mockSmsProvider;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${app.sms.provider:auto}")
    private String preferredProvider;

    @Value("${app.verification.base-url:http://localhost:4200/verify}")
    private String verificationBaseUrl;

    public SmsNotificationService(Fast2SmsProvider fast2SmsProvider,
                                  TwilioSmsProvider twilioSmsProvider,
                                  MockSmsProvider mockSmsProvider,
                                  NotificationLogRepository notificationLogRepository) {
        this.fast2SmsProvider = fast2SmsProvider;
        this.twilioSmsProvider = twilioSmsProvider;
        this.mockSmsProvider = mockSmsProvider;
        this.notificationLogRepository = notificationLogRepository;
    }

    private SmsProvider resolveProvider() {
        if ("fast2sms".equalsIgnoreCase(preferredProvider) && fast2SmsProvider.isConfigured()) {
            return fast2SmsProvider;
        } else if ("twilio".equalsIgnoreCase(preferredProvider) && twilioSmsProvider.isConfigured()) {
            return twilioSmsProvider;
        } else if (fast2SmsProvider.isConfigured()) {
            return fast2SmsProvider;
        } else if (twilioSmsProvider.isConfigured()) {
            return twilioSmsProvider;
        }
        return mockSmsProvider;
    }

    public SmsSendResult sendOtpSms(UserEntity user, String plainOtp, String destinationPhone) {
        String cleanPhone = normalizePhoneNumber(destinationPhone);
        String maskedPhone = maskPhoneNumber(cleanPhone);

        String message = String.format(
                "AI Shield Security: Your verification OTP is %s. Valid for 5 minutes. Do NOT share this security code with anyone.",
                plainOtp
        );

        boolean fast2SmsAvailable = fast2SmsProvider.isConfigured();
        boolean twilioAvailable = twilioSmsProvider.isConfigured();

        // 1. Strict validation: Real provider must be configured in environment
        if (!fast2SmsAvailable && !twilioAvailable) {
            log.error("[REAL SMS ERROR] Fast2SMS API key not configured in environment / .env file.");
            SmsSendResult noProviderResult = SmsSendResult.failure(
                    "FAST2SMS",
                    "Real SMS delivery required: Fast2SMS API Key (FAST2SMS_API_KEY) is not configured in your .env file."
            );
            recordAuditLog(user, maskedPhone, noProviderResult);
            return noProviderResult;
        }

        SmsSendResult result = null;

        // Preferred Twilio explicitly configured
        if ("twilio".equalsIgnoreCase(preferredProvider) && twilioAvailable) {
            result = twilioSmsProvider.sendSms(cleanPhone, message);
            if (!result.isSuccess() && fast2SmsAvailable) {
                log.warn("[SMS FAILOVER] Twilio dispatch failed ({}). Attempting failover to Fast2SMS...", result.getErrorMessage());
                result = fast2SmsProvider.sendSms(cleanPhone, message);
            }
        } else {
            // Default: Fast2SMS preferred
            if (fast2SmsAvailable) {
                result = fast2SmsProvider.sendSms(cleanPhone, message);
            }
            if ((result == null || !result.isSuccess()) && twilioAvailable) {
                if (result != null) {
                    log.warn("[SMS FAILOVER] Fast2SMS dispatch failed ({}). Attempting failover to Twilio...", result.getErrorMessage());
                }
                result = twilioSmsProvider.sendSms(cleanPhone, message);
            }
        }

        if (result == null) {
            result = SmsSendResult.failure("FAST2SMS", "Failed to dispatch SMS through configured provider.");
        }

        recordAuditLog(user, maskedPhone, result);
        return result;
    }

    private void recordAuditLog(UserEntity user, String maskedPhone, SmsSendResult result) {
        NotificationLogEntity logEntry = new NotificationLogEntity(
                user != null ? user.getId() : null,
                null,
                maskedPhone,
                "SMS_OTP",
                result.getStatus(),
                result.getProviderName(),
                result.getProviderReference()
        );
        if (!result.isSuccess()) {
            logEntry.setFailureReason(result.getErrorMessage());
        }
        notificationLogRepository.save(logEntry);
    }

    public SmsSendResult sendTransactionFraudAlert(TransactionEntity tx, TransactionVerificationEntity verif, UserEntity user) {
        String recipientPhone = (user != null && user.getPhone() != null && !user.getPhone().isBlank()) 
                ? user.getPhone() 
                : (verif != null ? verif.getCustomerPhone() : null);

        if (recipientPhone == null || recipientPhone.isBlank()) {
            recipientPhone = "+91 98111 22334";
        }

        String cleanPhone = normalizePhoneNumber(recipientPhone);
        String maskedPhone = maskPhoneNumber(cleanPhone);

        String last4 = "XXXX";
        if (verif != null && verif.getCardMasked() != null && verif.getCardMasked().length() >= 4) {
            last4 = verif.getCardMasked().substring(verif.getCardMasked().length() - 4);
        } else if (tx != null && tx.getCard() != null && tx.getCard().getCardLast4() != null) {
            last4 = tx.getCard().getCardLast4();
        }

        String verifyLink = verificationBaseUrl + "/" + (verif != null ? verif.getVerificationRequestId() : "VR-ALERT");
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));

        // Safe formatted SMS without CVV, full card number or sensitive secrets
        String message = String.format(
                "AI Shield Security Alert:\n" +
                "A transaction of ₹%.0f was detected on your card ending in %s at %s.\n" +
                "Location: %s.\n" +
                "Time: %s.\n" +
                "Risk Level: %s (%d/100).\n" +
                "Please verify whether this was authorized by you: %s",
                tx != null ? tx.getAmount() : (verif != null ? verif.getAmount() : 0.0),
                last4,
                tx != null && tx.getMerchantName() != null ? tx.getMerchantName() : (verif != null ? verif.getMerchantName() : "Merchant"),
                tx != null ? tx.getLocation() : (verif != null ? verif.getLocation() : "India"),
                timeStr,
                tx != null ? tx.getRiskLevel() : (verif != null ? verif.getRiskLevel() : "HIGH"),
                tx != null ? tx.getRiskScore() : (verif != null ? verif.getRiskScore() : 85),
                verifyLink
        );

        SmsProvider provider = resolveProvider();
        SmsSendResult result = provider.sendSms(cleanPhone, message);

        if (verif != null) {
            verif.setSmsStatus(result.getStatus());
            verif.setSmsContent(message);
        }

        NotificationLogEntity logEntry = new NotificationLogEntity(
                user != null ? user.getId() : (tx != null && tx.getUser() != null ? tx.getUser().getId() : null),
                tx != null ? tx.getId() : null,
                maskedPhone,
                "SMS_TRANSACTION_ALERT",
                result.getStatus(),
                result.getProviderName(),
                result.getProviderReference()
        );
        if (!result.isSuccess()) {
            logEntry.setFailureReason(result.getErrorMessage());
        }
        notificationLogRepository.save(logEntry);

        return result;
    }

    public String buildSmsMessage(String customerName, String cardMasked, Double amount,
                                  String merchantName, String location, String txRef,
                                  String riskLevel, Integer riskScore, String verificationRequestId) {
        String last4 = (cardMasked != null && cardMasked.length() >= 4)
                ? cardMasked.substring(cardMasked.length() - 4) : "XXXX";

        String verifyLink = verificationBaseUrl + "/" + verificationRequestId;
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));

        return String.format(
                "AI Shield Security Alert:\n" +
                "A transaction of ₹%.0f was detected on your card ending in %s at %s.\n" +
                "Location: %s.\n" +
                "Time: %s.\n" +
                "Risk Level: %s (%d/100).\n" +
                "Please verify whether this was authorized by you: %s",
                amount != null ? amount : 0.0,
                last4,
                merchantName != null ? merchantName : "Outlet",
                location != null ? location : "India",
                timeStr,
                riskLevel != null ? riskLevel : "High",
                riskScore != null ? riskScore : 85,
                verifyLink
        );
    }

    public boolean sendSms(String recipientPhone, String messageText) {
        String cleanPhone = normalizePhoneNumber(recipientPhone);
        SmsProvider provider = resolveProvider();
        SmsSendResult result = provider.sendSms(cleanPhone, messageText);
        return result.isSuccess();
    }

    public SmsProvider getCurrentProvider() {
        return resolveProvider();
    }

    public boolean isRealSmsMode() {
        return fast2SmsProvider.isConfigured() || twilioSmsProvider.isConfigured();
    }

    public String normalizePhoneNumber(String raw) {
        if (raw == null || raw.isBlank()) return "+919800000001";
        String clean = raw.replaceAll("[^0-9+]", "");
        if (!clean.startsWith("+") && clean.length() == 10) {
            clean = "+91" + clean;
        }
        return clean;
    }

    public String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "+91 ******0001";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "+91 ******0001";
        String last4 = digits.substring(digits.length() - 4);
        return "+91 ******" + last4;
    }
}
