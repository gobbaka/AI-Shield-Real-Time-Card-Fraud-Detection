package com.aishield.fraud.repository;

import com.aishield.fraud.entity.FraudRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRuleEntity, Long> {
    Optional<FraudRuleEntity> findByRuleKey(String ruleKey);
    List<FraudRuleEntity> findByEnabledTrue();
}
