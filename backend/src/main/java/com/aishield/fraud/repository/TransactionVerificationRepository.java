package com.aishield.fraud.repository;

import com.aishield.fraud.entity.TransactionVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionVerificationRepository extends JpaRepository<TransactionVerificationEntity, Long> {

    Optional<TransactionVerificationEntity> findByVerificationRequestId(String verificationRequestId);

    Optional<TransactionVerificationEntity> findByTransactionRef(String transactionRef);

    List<TransactionVerificationEntity> findByCustomerEmailAndStatus(String customerEmail, String status);

    List<TransactionVerificationEntity> findByStatus(String status);

    @Query("SELECT v FROM TransactionVerificationEntity v WHERE (:email IS NULL OR LOWER(v.customerEmail) = LOWER(:email) OR LOWER(v.customerName) LIKE LOWER(CONCAT('%', :email, '%'))) ORDER BY v.requestedAt DESC")
    List<TransactionVerificationEntity> findHistoryByCustomer(@Param("email") String email);

    @Query("SELECT v FROM TransactionVerificationEntity v WHERE v.status = 'PENDING' AND v.expiresAt < :now")
    List<TransactionVerificationEntity> findExpiredPendingVerifications(@Param("now") LocalDateTime now);

    @Query("SELECT v FROM TransactionVerificationEntity v WHERE v.status = 'PENDING' ORDER BY v.requestedAt DESC")
    List<TransactionVerificationEntity> findAllPending();
}
