package com.aishield.fraud.repository;

import com.aishield.fraud.entity.FraudAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudAuditLogRepository extends JpaRepository<FraudAuditLogEntity, Long> {

    List<FraudAuditLogEntity> findByTransactionRefOrderByTimestampDesc(String transactionRef);

    List<FraudAuditLogEntity> findByEventTypeOrderByTimestampDesc(String eventType);

    List<FraudAuditLogEntity> findByUserIdOrderByTimestampDesc(Long userId);

    Page<FraudAuditLogEntity> findAllByOrderByTimestampDesc(Pageable pageable);

    List<FraudAuditLogEntity> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}
