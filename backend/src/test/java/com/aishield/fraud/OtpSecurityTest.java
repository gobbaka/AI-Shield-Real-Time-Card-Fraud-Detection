package com.aishield.fraud;

import com.aishield.fraud.entity.OtpVerificationEntity;
import com.aishield.fraud.entity.UserEntity;
import com.aishield.fraud.exception.BadRequestException;
import com.aishield.fraud.repository.OtpVerificationRepository;
import com.aishield.fraud.repository.UserRepository;
import com.aishield.fraud.service.OtpVerificationService;
import com.aishield.fraud.service.SmsNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OtpSecurityTest {

    private OtpVerificationRepository otpRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private SmsNotificationService smsNotificationService;

    @BeforeEach
    void setUp() {
        otpRepository = mock(OtpVerificationRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        smsNotificationService = mock(SmsNotificationService.class);
    }

    @Test
    @DisplayName("Code 123456 is strictly rejected when not matching hashed OTP")
    void testDemoOtpBypassDisabledInProduction() {
        OtpVerificationService service = new OtpVerificationService(
                otpRepository, userRepository, passwordEncoder, smsNotificationService
        );

        String realOtp = "948210";
        String hashedOtp = passwordEncoder.encode(realOtp);
        OtpVerificationEntity entity = new OtpVerificationEntity(
                new UserEntity(), "+919876543210", "CH-SEC-1", "LOGIN",
                hashedOtp, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusSeconds(60)
        );

        when(otpRepository.findByChallengeId("CH-SEC-1")).thenReturn(Optional.of(entity));

        assertThrows(BadRequestException.class, () -> {
            service.verifyChallengeOtp("CH-SEC-1", "123456");
        }, "123456 must be rejected when not matching hash");

        assertEquals(1, entity.getAttempts());
    }

    @Test
    @DisplayName("Universal demo code 123456 is strictly rejected and increments attempt count")
    void testUniversalDemoOtpIsNeverAccepted() {
        OtpVerificationService service = new OtpVerificationService(
                otpRepository, userRepository, passwordEncoder, smsNotificationService
        );

        String realOtp = "948210";
        String hashedOtp = passwordEncoder.encode(realOtp);
        UserEntity user = new UserEntity();
        OtpVerificationEntity entity = new OtpVerificationEntity(
                user, "+919876543210", "CH-NO-BYPASS", "LOGIN",
                hashedOtp, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusSeconds(60)
        );

        when(otpRepository.findByChallengeId("CH-NO-BYPASS")).thenReturn(Optional.of(entity));

        assertThrows(BadRequestException.class, () -> {
            service.verifyChallengeOtp("CH-NO-BYPASS", "123456");
        }, "123456 must always be rejected when it doesn't match the hash");

        assertEquals(1, entity.getAttempts());
    }

    @Test
    @DisplayName("Exceeding max attempts locks challenge and rejects further attempts")
    void testMaxAttemptsLockout() {
        OtpVerificationService service = new OtpVerificationService(
                otpRepository, userRepository, passwordEncoder, smsNotificationService
        );

        String hashedOtp = passwordEncoder.encode("888888");
        OtpVerificationEntity entity = new OtpVerificationEntity(
                null, "+919876543210", "CH-LOCK-1", "LOGIN",
                hashedOtp, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusSeconds(60)
        );
        entity.setMaxAttempts(3);
        entity.setAttempts(2); // already at 2 of 3 attempts

        when(otpRepository.findByChallengeId("CH-LOCK-1")).thenReturn(Optional.of(entity));

        // Attempt 3 - fails
        assertThrows(BadRequestException.class, () -> {
            service.verifyChallengeOtp("CH-LOCK-1", "111111");
        });

        assertEquals(3, entity.getAttempts());
        assertEquals("MAX_ATTEMPTS_EXCEEDED", entity.getStatus());

        // Subsequent attempt must be blocked immediately
        assertThrows(BadRequestException.class, () -> {
            service.verifyChallengeOtp("CH-LOCK-1", "888888");
        });
    }

    @Test
    @DisplayName("Expired OTP challenge throws BadRequestException and marks status EXPIRED")
    void testExpiredChallengeRejected() {
        OtpVerificationService service = new OtpVerificationService(
                otpRepository, userRepository, passwordEncoder, smsNotificationService
        );

        String hashedOtp = passwordEncoder.encode("123987");
        OtpVerificationEntity entity = new OtpVerificationEntity(
                null, "+919876543210", "CH-EXP-1", "LOGIN",
                hashedOtp, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusMinutes(2)
        );

        when(otpRepository.findByChallengeId("CH-EXP-1")).thenReturn(Optional.of(entity));

        assertThrows(BadRequestException.class, () -> {
            service.verifyChallengeOtp("CH-EXP-1", "123987");
        });

        assertEquals("EXPIRED", entity.getStatus());
    }
}
