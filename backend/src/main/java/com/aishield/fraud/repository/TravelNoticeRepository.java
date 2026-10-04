package com.aishield.fraud.repository;

import com.aishield.fraud.entity.TravelNoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TravelNoticeRepository extends JpaRepository<TravelNoticeEntity, Long> {

    List<TravelNoticeEntity> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    @Query("SELECT t FROM TravelNoticeEntity t WHERE t.customerEmail = :email AND t.status = 'ACTIVE' AND :currentDate BETWEEN t.startDate AND t.endDate")
    List<TravelNoticeEntity> findActiveNoticesForCustomer(@Param("email") String email, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT t FROM TravelNoticeEntity t WHERE t.status = 'ACTIVE' AND :currentDate BETWEEN t.startDate AND t.endDate")
    List<TravelNoticeEntity> findAllActiveNotices(@Param("currentDate") LocalDate currentDate);
}