package com.aishield.fraud.service;

import com.aishield.fraud.dto.AuthDtos;
import com.aishield.fraud.entity.OtpVerificationEntity;
import com.aishield.fraud.entity.UserEntity;
import com.aishield.fraud.exception.BadRequestException;
import com.aishield.fraud.exception.ResourceNotFoundException;
import com.aishield.fraud.repository.UserRepository;
import com.aishield.fraud.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final OtpVerificationService otpVerificationService;
    private final SmsNotificationService smsNotificationService;
    private final boolean otpEnabled;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       OtpVerificationService otpVerificationService,
                       SmsNotificationService smsNotificationService,
                       @org.springframework.beans.factory.annotation.Value("${aishield.auth.otp-enabled:false}") boolean otpEnabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.otpVerificationService = otpVerificationService;
        this.smsNotificationService = smsNotificationService;
        this.otpEnabled = otpEnabled;
    }

    /**
     * STEP 1: Verify credentials and issue 2FA OTP Challenge.
     * If OTP is disabled in config, issues JWT session directly.
     */
    @Transactional
    public AuthDtos.LoginChallengeResponse initiateLogin(AuthDtos.LoginRequest request) {
        String identifier = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String rawPassword = request.getPassword() != null ? request.getPassword().trim() : "";

        if (identifier.isEmpty() || rawPassword.isEmpty()) {
            throw new BadRequestException("Please enter both email/phone and password.");
        }

        // Support lookup by email, phone, or standard username aliases
        String normalizedInput = smsNotificationService.normalizePhoneNumber(identifier);
        UserEntity user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> (u.getPhone() != null && normalizedInput.equals(smsNotificationService.normalizePhoneNumber(u.getPhone())))
                                || (u.getEmail() != null && (u.getEmail().equalsIgnoreCase(identifier)
                                        || u.getEmail().toLowerCase().startsWith(identifier.toLowerCase() + "@")))
                                || (u.getName() != null && u.getName().equalsIgnoreCase(identifier)))
                        .findFirst()
                        .orElse(null));

        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadRequestException("Invalid email/phone or password. Please check your credentials.");
        }

        // When OTP verification is disabled, authenticate directly and return authenticated JWT session
        if (!otpEnabled) {
            String jwtToken = tokenProvider.generateTokenForUser(user.getId(), user.getEmail(), user.getName());
            AuthDtos.AuthResponse authResponse = new AuthDtos.AuthResponse(
                    jwtToken,
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getPhone(),
                    user.getMobileVerified() != null ? user.getMobileVerified() : false
            );
            AuthDtos.LoginChallengeResponse directResponse = new AuthDtos.LoginChallengeResponse();
            directResponse.setStep("AUTHENTICATED");
            directResponse.setMessage("Authentication successful.");
            directResponse.setAuthResponse(authResponse);
            return directResponse;
        }

        // Generate temporary 2FA challenge
        String challengeId = UUID.randomUUID().toString();
        String targetPhone = user.getPhone() != null && !user.getPhone().isBlank() ? user.getPhone() : "+919876543210";

        AuthDtos.OtpResponse otpRes = otpVerificationService.generateAndSendChallengeOtp(user, targetPhone, challengeId, "LOGIN");
        String finalChallengeId = (otpRes.getChallengeId() != null && !otpRes.getChallengeId().isBlank()) ? otpRes.getChallengeId() : challengeId;

        return new AuthDtos.LoginChallengeResponse(
                finalChallengeId,
                "OTP_REQUIRED",
                otpRes.getDestinationMasked(),
                otpRes.getResendAvailableInSeconds(),
                300,
                otpRes.getMessage() != null ? otpRes.getMessage() : ("Credentials verified. One-time verification code sent to " + otpRes.getDestinationMasked())
        );
    }

    /**
     * STEP 2: Verify OTP challenge and issue final authenticated JWT session.
     */
    @Transactional
    public AuthDtos.AuthResponse verifyLoginChallenge(AuthDtos.VerifyLoginOtpRequest request) {
        OtpVerificationEntity verifiedEntity = otpVerificationService.verifyChallengeOtp(
                request.getChallengeId(), request.getOtp()
        );

        UserEntity user = verifiedEntity.getUser();
        if (user == null) {
            throw new BadRequestException("Authentication session expired. Please sign in again.");
        }

        user.setMobileVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Issue true cryptographic JWT session token
        String jwtToken = tokenProvider.generateTokenForUser(user.getId(), user.getEmail(), user.getName());

        return new AuthDtos.AuthResponse(
                jwtToken,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getMobileVerified()
        );
    }

    /**
     * Resend OTP for active login challenge.
     */
    @Transactional
    public AuthDtos.OtpResponse resendLoginOtp(AuthDtos.ResendLoginOtpRequest request) {
        return otpVerificationService.resendChallengeOtp(request.getChallengeId());
    }

    /**
     * Cardholder Registration: Creates account and dispatches mobile OTP.
     */
    @Transactional
    public AuthDtos.LoginChallengeResponse register(AuthDtos.RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BadRequestException("An account is already registered with email: " + cleanEmail);
        }

        String cleanPhone = smsNotificationService.normalizePhoneNumber(request.getPhone());
        if (cleanPhone.length() < 10) {
            throw new BadRequestException("Please enter a valid 10-digit mobile number.");
        }

        UserEntity user = new UserEntity(
                request.getName().trim(),
                cleanEmail,
                passwordEncoder.encode(request.getPassword().trim()),
                "CUSTOMER",
                cleanPhone,
                request.getLocation() != null ? request.getLocation().trim() : "India",
                "Verified Cardholder"
        );
        user.setMobileVerified(false);

        UserEntity saved = userRepository.save(user);

        if (!otpEnabled) {
            saved.setMobileVerified(true);
            userRepository.save(saved);
            String jwtToken = tokenProvider.generateTokenForUser(saved.getId(), saved.getEmail(), saved.getName());
            AuthDtos.AuthResponse authResponse = new AuthDtos.AuthResponse(
                    jwtToken,
                    saved.getId(),
                    saved.getName(),
                    saved.getEmail(),
                    saved.getRole(),
                    saved.getPhone(),
                    true
            );
            AuthDtos.LoginChallengeResponse directResponse = new AuthDtos.LoginChallengeResponse();
            directResponse.setStep("AUTHENTICATED");
            directResponse.setMessage("Account created and authenticated successfully.");
            directResponse.setAuthResponse(authResponse);
            return directResponse;
        }

        String challengeId = UUID.randomUUID().toString();
        AuthDtos.OtpResponse otpRes = otpVerificationService.generateAndSendChallengeOtp(saved, saved.getPhone(), challengeId, "REGISTRATION");

        return new AuthDtos.LoginChallengeResponse(
                challengeId,
                "OTP_REQUIRED",
                otpRes.getDestinationMasked(),
                otpRes.getResendAvailableInSeconds(),
                300,
                otpRes.getMessage() != null ? otpRes.getMessage() : ("Account created. Verification code sent to " + otpRes.getDestinationMasked())
        );
    }

    /**
     * Forgot Password: sends reset OTP challenge.
     */
    @Transactional
    public AuthDtos.LoginChallengeResponse forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        if (!otpEnabled) {
            throw new BadRequestException("SMS OTP verification is temporarily inactive. Please sign in directly with your account credentials.");
        }
        String identifier = request.getEmailOrPhone().trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> u.getPhone() != null && identifier.equals(smsNotificationService.normalizePhoneNumber(u.getPhone())))
                        .findFirst()
                        .orElse(null));

        if (user == null) {
            // Mask response to prevent user enumeration
            return new AuthDtos.LoginChallengeResponse(
                    UUID.randomUUID().toString(),
                    "OTP_REQUIRED",
                    "+91 ******0000",
                    60,
                    300,
                    "If an account is associated with this detail, a verification code has been dispatched."
            );
        }

        String challengeId = UUID.randomUUID().toString();
        AuthDtos.OtpResponse otpRes = otpVerificationService.generateAndSendChallengeOtp(user, user.getPhone(), challengeId, "PASSWORD_RESET");

        return new AuthDtos.LoginChallengeResponse(
                challengeId,
                "OTP_REQUIRED",
                otpRes.getDestinationMasked(),
                otpRes.getResendAvailableInSeconds(),
                300,
                otpRes.getMessage() != null ? otpRes.getMessage() : ("Password reset code dispatched to " + otpRes.getDestinationMasked())
        );
    }

    /**
     * Reset Password: verifies OTP and sets new password.
     */
    @Transactional
    public AuthDtos.AuthResponse resetPassword(AuthDtos.ResetPasswordRequest request) {
        OtpVerificationEntity verifiedEntity = otpVerificationService.verifyChallengeOtp(
                request.getChallengeId(), request.getOtp()
        );

        UserEntity user = verifiedEntity.getUser();
        if (user == null) {
            throw new BadRequestException("Password reset session expired. Please request a new code.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String jwtToken = tokenProvider.generateTokenForUser(user.getId(), user.getEmail(), user.getName());
        return new AuthDtos.AuthResponse(
                jwtToken,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getMobileVerified()
        );
    }

    public AuthDtos.UserProfileDto getProfile(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return new AuthDtos.UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getMobileVerified(),
                user.getLocation(),
                user.getBio()
        );
    }

    @Transactional
    public AuthDtos.UserProfileDto updateProfile(String email, AuthDtos.UserProfileDto updateDto) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (updateDto.getName() != null) user.setName(updateDto.getName());
        if (updateDto.getLocation() != null) user.setLocation(updateDto.getLocation());
        if (updateDto.getBio() != null) user.setBio(updateDto.getBio());

        user.setUpdatedAt(LocalDateTime.now());
        UserEntity updated = userRepository.save(user);

        return new AuthDtos.UserProfileDto(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                updated.getRole(),
                updated.getPhone(),
                updated.getMobileVerified(),
                updated.getLocation(),
                updated.getBio()
        );
    }
}
