package com.aishield.fraud.repository;

import com.aishield.fraud.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<CardEntity, Long> {
    List<CardEntity> findByUserId(Long userId);
    Optional<CardEntity> findByCardLast4(String cardLast4);
    Optional<CardEntity> findByCardNumberMasked(String cardNumberMasked);
}
