package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications", indexes = {
        @Index(name = "idx_otp_destination", columnList = "destination"),
        @Index(name = "idx_otp_status", columnList = "status"),
        @Index(name = "idx_otp_user_id", columnList = "user_id")
})
public class OtpVerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private String destination; // Mobile Number (e.g. "+919811122334") or email

    @Column(unique = true)
    private String challengeId; // UUID for 2FA login/registration challenge

    private String purpose = "LOGIN"; // "LOGIN", "REGISTRATION", "PASSWORD_RESET", "TRANSACTION"

    @Column(nullable = false)
    private String otpHash; // BCrypt hash of the 6-digit OTP

    @Column(nullable = false)
    private String status = "PENDING"; // "PENDING", "VERIFIED", "EXPIRED", "MAX_ATTEMPTS_EXCEEDED"

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false)
    private Integer maxAttempts = 5;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime resendCooldownUntil;

    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public OtpVerificationEntity() {}

    public OtpVerificationEntity(UserEntity user, String destination, String otpHash, LocalDateTime expiresAt, LocalDateTime resendCooldownUntil) {
        this(user, destination, null, "LOGIN", otpHash, expiresAt, resendCooldownUntil);
    }

    public OtpVerificationEntity(UserEntity user, String destination, String challengeId, String purpose, String otpHash, LocalDateTime expiresAt, LocalDateTime resendCooldownUntil) {
        this.user = user;
        this.destination = destination;
        this.challengeId = challengeId;
        this.purpose = purpose;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.resendCooldownUntil = resendCooldownUntil;
        this.status = "PENDING";
        this.attempts = 0;
        this.maxAttempts = 5;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }

    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getResendCooldownUntil() { return resendCooldownUntil; }
    public void setResendCooldownUntil(LocalDateTime resendCooldownUntil) { this.resendCooldownUntil = resendCooldownUntil; }

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
