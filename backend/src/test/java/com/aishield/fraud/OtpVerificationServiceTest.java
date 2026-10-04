package com.aishield.fraud;

import com.aishield.fraud.dto.AuthDtos;
import com.aishield.fraud.entity.OtpVerificationEntity;
import com.aishield.fraud.entity.UserEntity;
import com.aishield.fraud.exception.BadRequestException;
import com.aishield.fraud.repository.OtpVerificationRepository;
import com.aishield.fraud.repository.UserRepository;
import com.aishield.fraud.service.OtpVerificationService;
import com.aishield.fraud.service.SmsNotificationService;
import com.aishield.fraud.sms.SmsSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OtpVerificationServiceTest {

    private OtpVerificationRepository otpRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private SmsNotificationService smsNotificationService;
    private OtpVerificationService otpVerificationService;

    @BeforeEach
    void setUp() {
        otpRepository = mock(OtpVerificationRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        smsNotificationService = mock(SmsNotificationService.class);

        when(smsNotificationService.normalizePhoneNumber(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).replaceAll("[^0-9+]", ""));
        when(smsNotificationService.maskPhoneNumber(anyString()))
                .thenReturn("+91 98XXX 22334");
        when(smsNotificationService.sendOtpSms(any(), anyString(), anyString()))
                .thenReturn(SmsSendResult.success("FAST2SMS", "REQ-12345"));

        otpVerificationService = new OtpVerificationService(
                otpRepository, userRepository, passwordEncoder, smsNotificationService
        );
    }

    @Test
    void testGenerateOtp_Success() {
        UserEntity user = new UserEntity("Test User", "test@example.com", "hash", "CUSTOMER", "+919800000001", "India", "Bio");
        when(otpRepository.findActiveByDestination(anyString(), any())).thenReturn(Collections.emptyList());

        AuthDtos.OtpResponse response = otpVerificationService.generateAndSendChallengeOtp(user, "+919800000001", "CH-123", "LOGIN");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(60, response.getResendAvailableInSeconds());
        verify(otpRepository, times(1)).save(any(OtpVerificationEntity.class));
        verify(smsNotificationService, times(1)).sendOtpSms(eq(user), anyString(), anyString());
    }

    @Test
    void testVerifyOtp_Success() {
        String plainOtp = "849201";
        String hashedOtp = passwordEncoder.encode(plainOtp);
        UserEntity user = new UserEntity("Test User", "test@example.com", "hash", "CUSTOMER", "+919800000001", "India", "Bio");

        OtpVerificationEntity entity = new OtpVerificationEntity(
                user, "+919800000001", "CH-123", "LOGIN", hashedOtp, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusSeconds(60)
        );

        when(otpRepository.findByChallengeId(eq("CH-123")))
                .thenReturn(java.util.Optional.of(entity));

        OtpVerificationEntity result = otpVerificationService.verifyChallengeOtp("CH-123", "849201");

        assertNotNull(result);
        assertEquals("VERIFIED", result.getStatus());
        assertTrue(user.getMobileVerified());
    }

    @Test
    void testVerifyOtp_InvalidCode_IncrementsAttempts() {
        String hashedOtp = passwordEncoder.encode("999999");
        OtpVerificationEntity entity = new OtpVerificationEntity(
                null, "+919800000001", "CH-123", "LOGIN", hashedOtp, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusSeconds(60)
        );

        when(otpRepository.findByChallengeId(eq("CH-123")))
                .thenReturn(java.util.Optional.of(entity));

        assertThrows(BadRequestException.class, () -> {
            otpVerificationService.verifyChallengeOtp("CH-123", "000000");
        });

        assertEquals(1, entity.getAttempts());
    }

    @Test
    void testVerifyOtp_123456FailsWhenNotMatchingHash() {
        String hashedOtp = passwordEncoder.encode("772211");
        OtpVerificationEntity entity = new OtpVerificationEntity(
                null, "+919800000001", "CH-DEMO-CHECK", "LOGIN", hashedOtp, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusSeconds(60)
        );

        when(otpRepository.findByChallengeId(eq("CH-DEMO-CHECK")))
                .thenReturn(java.util.Optional.of(entity));

        assertThrows(BadRequestException.class, () -> {
            otpVerificationService.verifyChallengeOtp("CH-DEMO-CHECK", "123456");
        });

        assertEquals(1, entity.getAttempts());
    }

    @Test
    void testGenerateOtp_SmsFailure_RollsBackAndThrowsException() {
        UserEntity user = new UserEntity("Test User", "test@example.com", "hash", "CUSTOMER", "+919800000001", "India", "Bio");
        when(otpRepository.findActiveByDestination(anyString(), any())).thenReturn(Collections.emptyList());
        when(smsNotificationService.sendOtpSms(any(), anyString(), anyString()))
                .thenReturn(SmsSendResult.failure("SMS_GATEWAY", "Real SMS delivery is required, but neither Fast2SMS nor Twilio is configured"));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            otpVerificationService.generateAndSendChallengeOtp(user, "+919800000001", "CH-FAIL-1", "LOGIN");
        });

        assertTrue(ex.getMessage().contains("Real SMS delivery is required"));
        verify(otpRepository, times(1)).delete(any(OtpVerificationEntity.class));
    }
}
