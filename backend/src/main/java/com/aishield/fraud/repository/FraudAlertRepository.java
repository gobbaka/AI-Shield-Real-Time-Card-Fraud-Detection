package com.aishield.fraud.repository;

import com.aishield.fraud.entity.FraudAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlertEntity, Long> {

    Optional<FraudAlertEntity> findByAlertRef(String alertRef);

    List<FraudAlertEntity> findAllByOrderByCreatedAtDesc();

    @Query("SELECT a FROM FraudAlertEntity a WHERE " +
           "(:query IS NULL OR LOWER(a.alertRef) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(a.customerName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(a.location) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:status IS NULL OR a.status = :status) ORDER BY a.createdAt DESC")
    List<FraudAlertEntity> searchAlerts(@Param("query") String query, @Param("status") String status);

    long countByStatus(String status);

    @Query("SELECT a.location, COUNT(a) FROM FraudAlertEntity a GROUP BY a.location ORDER BY COUNT(a) DESC")
    List<Object[]> findTopFraudLocations();
}
