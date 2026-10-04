package com.aishield.fraud.controller;

import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.dto.AuthDtos;
import com.aishield.fraud.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * STEP 1: Verify credentials and issue 2FA Challenge.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.LoginChallengeResponse>> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.LoginChallengeResponse challengeResponse = authService.initiateLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Credentials verified. Identity verification code dispatched.", challengeResponse));
    }

    /**
     * STEP 2: Verify OTP and issue final authenticated JWT token.
     */
    @PostMapping("/verify-login-otp")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> verifyLoginOtp(@Valid @RequestBody AuthDtos.VerifyLoginOtpRequest request) {
        AuthDtos.AuthResponse response = authService.verifyLoginChallenge(request);
        return ResponseEntity.ok(ApiResponse.success("Identity verified successfully. Authenticated session established.", response));
    }

    /**
     * Resend 2FA OTP for active login challenge.
     */
    @PostMapping("/resend-login-otp")
    public ResponseEntity<ApiResponse<AuthDtos.OtpResponse>> resendLoginOtp(@Valid @RequestBody AuthDtos.ResendLoginOtpRequest request) {
        AuthDtos.OtpResponse response = authService.resendLoginOtp(request);
        return ResponseEntity.ok(ApiResponse.success("New verification code dispatched.", response));
    }

    /**
     * Registration: creates cardholder profile and issues verification challenge.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDtos.LoginChallengeResponse>> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        AuthDtos.LoginChallengeResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration initiated. Verification OTP sent to mobile.", response));
    }

    /**
     * Forgot Password: sends reset OTP challenge.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<AuthDtos.LoginChallengeResponse>> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        AuthDtos.LoginChallengeResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset challenge dispatched.", response));
    }

    /**
     * Reset Password: verifies OTP and sets new password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        AuthDtos.AuthResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. Authenticated session established.", response));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AuthDtos.UserProfileDto>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : "pradeep@example.com";
        AuthDtos.UserProfileDto profile = authService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthDtos.UserProfileDto>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AuthDtos.UserProfileDto dto) {
        String email = userDetails != null ? userDetails.getUsername() : "pradeep@example.com";
        AuthDtos.UserProfileDto updated = authService.updateProfile(email, dto);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }
}

