package com.aishield.fraud.repository;

import com.aishield.fraud.entity.NotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> {

    List<NotificationLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationLogEntity> findByTransactionIdOrderByCreatedAtDesc(Long transactionId);
}
