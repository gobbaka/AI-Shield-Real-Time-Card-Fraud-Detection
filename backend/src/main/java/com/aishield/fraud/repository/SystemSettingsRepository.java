package com.aishield.fraud.repository;

import com.aishield.fraud.entity.SystemSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettingsEntity, Long> {
    Optional<SystemSettingsEntity> findFirstByOrderByIdAsc();
}
