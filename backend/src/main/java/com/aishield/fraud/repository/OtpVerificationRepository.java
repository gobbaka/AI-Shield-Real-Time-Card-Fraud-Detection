package com.aishield.fraud.repository;

import com.aishield.fraud.entity.OtpVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerificationEntity, Long> {

    @Query("SELECT o FROM OtpVerificationEntity o WHERE o.destination = :destination AND o.status = 'PENDING' AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    List<OtpVerificationEntity> findActiveByDestination(String destination, LocalDateTime now);

    Optional<OtpVerificationEntity> findByChallengeId(String challengeId);

    Optional<OtpVerificationEntity> findTopByDestinationOrderByCreatedAtDesc(String destination);

    List<OtpVerificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
