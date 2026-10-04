package com.aishield.fraud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public LoginRequest() {}
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginChallengeResponse {
        private String challengeId;
        private String step = "OTP_REQUIRED";
        private String destinationMasked;
        private Integer resendAvailableInSeconds = 60;
        private Integer expiresInSeconds = 300;
        private String message;
        private AuthResponse authResponse;

        public LoginChallengeResponse() {}
        public LoginChallengeResponse(String challengeId, String step, String destinationMasked, Integer resendAvailableInSeconds, Integer expiresInSeconds, String message) {
            this.challengeId = challengeId;
            this.step = step;
            this.destinationMasked = destinationMasked;
            this.resendAvailableInSeconds = resendAvailableInSeconds;
            this.expiresInSeconds = expiresInSeconds;
            this.message = message;
        }

        public String getChallengeId() { return challengeId; }
        public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
        public String getStep() { return step; }
        public void setStep(String step) { this.step = step; }
        public String getDestinationMasked() { return destinationMasked; }
        public void setDestinationMasked(String destinationMasked) { this.destinationMasked = destinationMasked; }
        public Integer getResendAvailableInSeconds() { return resendAvailableInSeconds; }
        public void setResendAvailableInSeconds(Integer resendAvailableInSeconds) { this.resendAvailableInSeconds = resendAvailableInSeconds; }
        public Integer getExpiresInSeconds() { return expiresInSeconds; }
        public void setExpiresInSeconds(Integer expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public AuthResponse getAuthResponse() { return authResponse; }
        public void setAuthResponse(AuthResponse authResponse) { this.authResponse = authResponse; }
    }

    public static class VerifyLoginOtpRequest {
        @NotBlank(message = "Challenge ID is required")
        private String challengeId;

        @NotBlank(message = "6-digit OTP code is required")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        private String otp;

        public VerifyLoginOtpRequest() {}
        public VerifyLoginOtpRequest(String challengeId, String otp) {
            this.challengeId = challengeId;
            this.otp = otp;
        }

        public String getChallengeId() { return challengeId; }
        public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class ResendLoginOtpRequest {
        @NotBlank(message = "Challenge ID is required")
        private String challengeId;

        public ResendLoginOtpRequest() {}
        public ResendLoginOtpRequest(String challengeId) {
            this.challengeId = challengeId;
        }

        public String getChallengeId() { return challengeId; }
        public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    }

    public static class RegisterRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank(message = "Mobile number is mandatory for security verification")
        private String phone;

        private String location = "India";

        public RegisterRequest() {}
        public RegisterRequest(String name, String email, String password, String phone) {
            this.name = name;
            this.email = email;
            this.password = password;
            this.phone = phone;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email or mobile number is required")
        private String emailOrPhone;

        public ForgotPasswordRequest() {}
        public ForgotPasswordRequest(String emailOrPhone) {
            this.emailOrPhone = emailOrPhone;
        }

        public String getEmailOrPhone() { return emailOrPhone; }
        public void setEmailOrPhone(String emailOrPhone) { this.emailOrPhone = emailOrPhone; }
    }

    public static class ResetPasswordRequest {
        @NotBlank(message = "Challenge ID is required")
        private String challengeId;

        @NotBlank(message = "6-digit OTP code is required")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        private String otp;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String newPassword;

        public ResetPasswordRequest() {}
        public ResetPasswordRequest(String challengeId, String otp, String newPassword) {
            this.challengeId = challengeId;
            this.otp = otp;
            this.newPassword = newPassword;
        }

        public String getChallengeId() { return challengeId; }
        public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class SendOtpRequest {
        @NotBlank(message = "Destination phone or email is required")
        private String destination;

        public SendOtpRequest() {}
        public SendOtpRequest(String destination) { this.destination = destination; }

        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
    }

    public static class VerifyOtpRequest {
        @NotBlank(message = "Destination is required")
        private String destination;

        @NotBlank(message = "6-digit OTP code is required")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        private String otp;

        public VerifyOtpRequest() {}
        public VerifyOtpRequest(String destination, String otp) {
            this.destination = destination;
            this.otp = otp;
        }

        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class OtpResponse {
        private boolean success;
        private String message;
        private String destinationMasked;
        private Integer resendAvailableInSeconds;
        private String expiresAt;
        private String challengeId;

        public OtpResponse() {}
        public OtpResponse(boolean success, String message, String destinationMasked, Integer resendAvailableInSeconds, String expiresAt) {
            this.success = success;
            this.message = message;
            this.destinationMasked = destinationMasked;
            this.resendAvailableInSeconds = resendAvailableInSeconds;
            this.expiresAt = expiresAt;
        }

        public OtpResponse(boolean success, String message, String destinationMasked, Integer resendAvailableInSeconds, String expiresAt, String challengeId) {
            this.success = success;
            this.message = message;
            this.destinationMasked = destinationMasked;
            this.resendAvailableInSeconds = resendAvailableInSeconds;
            this.expiresAt = expiresAt;
            this.challengeId = challengeId;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getDestinationMasked() { return destinationMasked; }
        public void setDestinationMasked(String destinationMasked) { this.destinationMasked = destinationMasked; }
        public Integer getResendAvailableInSeconds() { return resendAvailableInSeconds; }
        public void setResendAvailableInSeconds(Integer resendAvailableInSeconds) { this.resendAvailableInSeconds = resendAvailableInSeconds; }
        public String getExpiresAt() { return expiresAt; }
        public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
        public String getChallengeId() { return challengeId; }
        public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    }

    public static class OtpVerificationResult {
        private boolean success;
        private String message;
        private String mobileNumberMasked;
        private boolean verified;
        private String verifiedAt;

        public OtpVerificationResult() {}
        public OtpVerificationResult(boolean success, String message, String mobileNumberMasked, boolean verified, String verifiedAt) {
            this.success = success;
            this.message = message;
            this.mobileNumberMasked = mobileNumberMasked;
            this.verified = verified;
            this.verifiedAt = verifiedAt;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getMobileNumberMasked() { return mobileNumberMasked; }
        public void setMobileNumberMasked(String mobileNumberMasked) { this.mobileNumberMasked = mobileNumberMasked; }
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        public String getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(String verifiedAt) { this.verifiedAt = verifiedAt; }
    }

    public static class AuthResponse {
        private String token;
        private String tokenType = "Bearer";
        private Long userId;
        private String name;
        private String email;
        private String role;
        private String phone;
        private Boolean mobileVerified;
        private boolean otpRequired = false;

        public AuthResponse() {}
        public AuthResponse(String token, Long userId, String name, String email, String role, String phone, Boolean mobileVerified) {
            this.token = token;
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.phone = phone;
            this.mobileVerified = mobileVerified;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Boolean getMobileVerified() { return mobileVerified; }
        public void setMobileVerified(Boolean mobileVerified) { this.mobileVerified = mobileVerified; }
        public boolean isOtpRequired() { return otpRequired; }
        public void setOtpRequired(boolean otpRequired) { this.otpRequired = otpRequired; }
    }

    public static class UserProfileDto {
        private Long id;
        private String name;
        private String email;
        private String role;
        private String phone;
        private Boolean mobileVerified;
        private String location;
        private String bio;

        public UserProfileDto() {}
        public UserProfileDto(Long id, String name, String email, String role, String phone, Boolean mobileVerified, String location, String bio) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.phone = phone;
            this.mobileVerified = mobileVerified;
            this.location = location;
            this.bio = bio;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Boolean getMobileVerified() { return mobileVerified; }
        public void setMobileVerified(Boolean mobileVerified) { this.mobileVerified = mobileVerified; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
    }
}

