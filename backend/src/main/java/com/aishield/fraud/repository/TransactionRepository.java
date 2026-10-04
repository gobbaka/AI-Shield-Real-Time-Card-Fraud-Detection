package com.aishield.fraud.repository;

import com.aishield.fraud.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByTransactionRef(String transactionRef);

    List<TransactionEntity> findTop10ByOrderByTimestampDesc();

    List<TransactionEntity> findByUserIdOrderByTimestampDesc(Long userId);

    List<TransactionEntity> findByCardIdOrderByTimestampDesc(Long cardId);

    @Query("SELECT t FROM TransactionEntity t WHERE (:email IS NULL OR LOWER(t.user.email) = LOWER(:email) OR LOWER(t.customerName) LIKE LOWER(CONCAT('%', :email, '%'))) ORDER BY t.timestamp DESC")
    List<TransactionEntity> findByUserEmailOrCustomerName(@Param("email") String email);

    @Query("SELECT t FROM TransactionEntity t WHERE t.card.id = :cardId AND t.timestamp >= :since ORDER BY t.timestamp DESC")
    List<TransactionEntity> findRecentByCardId(@Param("cardId") Long cardId, @Param("since") LocalDateTime since);

    @Query("SELECT t FROM TransactionEntity t WHERE t.user.id = :userId AND t.timestamp >= :since ORDER BY t.timestamp DESC")
    List<TransactionEntity> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT t FROM TransactionEntity t WHERE " +
           "(:query IS NULL OR LOWER(t.transactionRef) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(t.customerName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(t.location) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:startDate IS NULL OR t.timestamp >= :startDate) " +
           "AND (:endDate IS NULL OR t.timestamp <= :endDate)")
    Page<TransactionEntity> searchTransactions(@Param("query") String query,
                                               @Param("status") String status,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM TransactionEntity t")
    Double sumTotalAmount();

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM TransactionEntity t WHERE t.status = 'Blocked'")
    Double sumBlockedAmount();

    @Query("SELECT COALESCE(AVG(t.riskScore), 0.0) FROM TransactionEntity t")
    Double averageRiskScore();
}
