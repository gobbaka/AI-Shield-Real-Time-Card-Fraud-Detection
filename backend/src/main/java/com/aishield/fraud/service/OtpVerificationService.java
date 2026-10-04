package com.aishield.fraud.service;

import com.aishield.fraud.dto.AuthDtos;
import com.aishield.fraud.entity.OtpVerificationEntity;
import com.aishield.fraud.entity.UserEntity;
import com.aishield.fraud.exception.BadRequestException;
import com.aishield.fraud.exception.ResourceNotFoundException;
import com.aishield.fraud.repository.OtpVerificationRepository;
import com.aishield.fraud.repository.UserRepository;
import com.aishield.fraud.sms.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OtpVerificationService {

    private static final Logger log = LoggerFactory.getLogger(OtpVerificationService.class);

    private final OtpVerificationRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsNotificationService smsNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public OtpVerificationService(OtpVerificationRepository otpRepository,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  SmsNotificationService smsNotificationService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsNotificationService = smsNotificationService;
    }

    @Deprecated
    public OtpVerificationService(OtpVerificationRepository otpRepository,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  SmsNotificationService smsNotificationService,
                                  boolean allowDemoOtpBypass) {
        this(otpRepository, userRepository, passwordEncoder, smsNotificationService);
    }

    @Transactional
    public AuthDtos.OtpResponse generateAndSendChallengeOtp(UserEntity user, String rawPhone, String challengeId, String purpose) {
        String cleanPhone = smsNotificationService.normalizePhoneNumber(rawPhone);
        LocalDateTime now = LocalDateTime.now();

        // 1. Check rate limit / resend cooldown on this destination
        List<OtpVerificationEntity> activeList = otpRepository.findActiveByDestination(cleanPhone, now);
        if (!activeList.isEmpty()) {
            OtpVerificationEntity active = activeList.get(0);
            if (active.getResendCooldownUntil() != null && active.getResendCooldownUntil().isAfter(now)) {
                // Return existing pending challenge so the user transitions to Step 2 smoothly
                if ("PENDING".equalsIgnoreCase(active.getStatus()) && active.getChallengeId() != null) {
                    long secondsLeft = java.time.Duration.between(now, active.getResendCooldownUntil()).toSeconds();
                    String masked = smsNotificationService.maskPhoneNumber(cleanPhone);
                    return new AuthDtos.OtpResponse(
                            true,
                            "Verification code dispatched to " + masked,
                            masked,
                            (int) Math.max(1, secondsLeft),
                            active.getExpiresAt().toString(),
                            active.getChallengeId()
                    );
                }
            }
            // Invalidate older active OTPs for this phone
            for (OtpVerificationEntity old : activeList) {
                old.setStatus("EXPIRED");
                otpRepository.save(old);
            }
        }

        // 2. Generate secure 6-digit cryptographic OTP
        int otpInt = 100000 + secureRandom.nextInt(900000);
        String plainOtp = String.valueOf(otpInt);

        // 3. Hash OTP with BCrypt
        String hashedOtp = passwordEncoder.encode(plainOtp);

        LocalDateTime expiresAt = now.plusMinutes(5);
        LocalDateTime cooldownUntil = now.plusSeconds(60);

        OtpVerificationEntity otpEntity = new OtpVerificationEntity(
                user, cleanPhone, challengeId, purpose, hashedOtp, expiresAt, cooldownUntil
        );
        otpRepository.save(otpEntity);

        // 4. Send real SMS via provider
        SmsSendResult sendResult = smsNotificationService.sendOtpSms(user, plainOtp, cleanPhone);
        String masked = smsNotificationService.maskPhoneNumber(cleanPhone);

        if (!sendResult.isSuccess()) {
            otpRepository.delete(otpEntity);
            String errorDetail = sendResult.getErrorMessage() != null ? sendResult.getErrorMessage() : "SMS dispatch failed";
            log.error("SMS delivery failed for challenge {}: {}", challengeId, errorDetail);
            throw new BadRequestException("Failed to send verification SMS to " + masked + ": " + errorDetail);
        }

        return new AuthDtos.OtpResponse(
                true,
                "Verification code dispatched to " + masked + ". Check your mobile phone SMS.",
                masked,
                60,
                expiresAt.toString(),
                challengeId
        );
    }

    @Transactional
    public OtpVerificationEntity verifyChallengeOtp(String challengeId, String otp) {
        if (otp == null || otp.trim().length() != 6) {
            throw new BadRequestException("Please enter a valid 6-digit OTP.");
        }

        LocalDateTime now = LocalDateTime.now();
        OtpVerificationEntity otpEntity = otpRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> new BadRequestException("Invalid or expired authentication challenge. Please sign in again."));

        if (!"PENDING".equalsIgnoreCase(otpEntity.getStatus()) || otpEntity.getExpiresAt().isBefore(now)) {
            otpEntity.setStatus("EXPIRED");
            otpRepository.save(otpEntity);
            throw new BadRequestException("Verification code has expired. Please request a new code.");
        }

        if (otpEntity.getAttempts() >= otpEntity.getMaxAttempts()) {
            otpEntity.setStatus("MAX_ATTEMPTS_EXCEEDED");
            otpRepository.save(otpEntity);
            throw new BadRequestException("Maximum verification attempts exceeded. Please restart sign-in.");
        }

        boolean matchesHash = passwordEncoder.matches(otp.trim(), otpEntity.getOtpHash());

        if (!matchesHash) {
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            if (otpEntity.getAttempts() >= otpEntity.getMaxAttempts()) {
                otpEntity.setStatus("MAX_ATTEMPTS_EXCEEDED");
            }
            otpRepository.save(otpEntity);
            int remaining = Math.max(0, otpEntity.getMaxAttempts() - otpEntity.getAttempts());
            throw new BadRequestException("Invalid verification code. " + remaining + " attempts remaining.");
        }

        // OTP Verified successfully - Invalidate immediately to prevent replay
        otpEntity.setStatus("VERIFIED");
        otpEntity.setVerifiedAt(now);
        otpRepository.save(otpEntity);

        // Mark user mobile as verified
        UserEntity user = otpEntity.getUser();
        if (user != null) {
            user.setMobileVerified(true);
            user.setMobileVerifiedAt(now);
            user.setUpdatedAt(now);
            userRepository.save(user);
        }

        return otpEntity;
    }

    @Transactional
    public AuthDtos.OtpResponse resendChallengeOtp(String challengeId) {
        OtpVerificationEntity existing = otpRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> new BadRequestException("Authentication session not found. Please log in again."));

        LocalDateTime now = LocalDateTime.now();
        if (existing.getResendCooldownUntil() != null && existing.getResendCooldownUntil().isAfter(now)) {
            long secondsLeft = java.time.Duration.between(now, existing.getResendCooldownUntil()).toSeconds();
            throw new BadRequestException("Please wait " + secondsLeft + " seconds before requesting a new code.");
        }

        // Expire existing
        existing.setStatus("EXPIRED");
        otpRepository.save(existing);

        // Generate new OTP with same challengeId
        int otpInt = 100000 + secureRandom.nextInt(900000);
        String plainOtp = String.valueOf(otpInt);
        String hashedOtp = passwordEncoder.encode(plainOtp);

        LocalDateTime expiresAt = now.plusMinutes(5);
        LocalDateTime cooldownUntil = now.plusSeconds(60);

        OtpVerificationEntity newEntity = new OtpVerificationEntity(
                existing.getUser(), existing.getDestination(), challengeId, existing.getPurpose(),
                hashedOtp, expiresAt, cooldownUntil
        );
        otpRepository.save(newEntity);

        SmsSendResult sendResult = smsNotificationService.sendOtpSms(existing.getUser(), plainOtp, existing.getDestination());
        String masked = smsNotificationService.maskPhoneNumber(existing.getDestination());

        if (!sendResult.isSuccess()) {
            otpRepository.delete(newEntity);
            String errorDetail = sendResult.getErrorMessage() != null ? sendResult.getErrorMessage() : "SMS dispatch failed";
            log.error("Resend SMS delivery failed for challenge {}: {}", challengeId, errorDetail);
            throw new BadRequestException("Failed to send verification SMS to " + masked + ": " + errorDetail);
        }

        return new AuthDtos.OtpResponse(
                true,
                "New verification code dispatched to " + masked + ". Check your mobile phone SMS.",
                masked,
                60,
                expiresAt.toString(),
                challengeId
        );
    }
}
