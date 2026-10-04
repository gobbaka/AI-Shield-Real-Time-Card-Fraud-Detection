package com.aishield.fraud.service;

import com.aishield.fraud.dto.SettingsDtos;
import com.aishield.fraud.entity.SystemSettingsEntity;
import com.aishield.fraud.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SettingsService {

    private final SystemSettingsRepository settingsRepository;
    private final com.aishield.fraud.repository.FraudRuleRepository fraudRuleRepository;

    public SettingsService(SystemSettingsRepository settingsRepository,
                           com.aishield.fraud.repository.FraudRuleRepository fraudRuleRepository) {
        this.settingsRepository = settingsRepository;
        this.fraudRuleRepository = fraudRuleRepository;
    }

    public SettingsDtos.SettingsDto getSettings() {
        SystemSettingsEntity entity = settingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> settingsRepository.save(new SystemSettingsEntity(false, true, true, true, 80)));

        return new SettingsDtos.SettingsDto(
                entity.getDarkMode(),
                entity.getNotifications(),
                entity.getAiDetection(),
                entity.getAutoBlock(),
                entity.getThreshold()
        );
    }

    @Transactional
    public SettingsDtos.SettingsDto updateSettings(SettingsDtos.SettingsDto dto) {
        SystemSettingsEntity entity = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(new SystemSettingsEntity());

        if (dto.getDarkMode() != null) entity.setDarkMode(dto.getDarkMode());
        if (dto.getNotifications() != null) entity.setNotifications(dto.getNotifications());
        if (dto.getAiDetection() != null) entity.setAiDetection(dto.getAiDetection());
        if (dto.getAutoBlock() != null) entity.setAutoBlock(dto.getAutoBlock());
        if (dto.getThreshold() != null) entity.setThreshold(dto.getThreshold());

        SystemSettingsEntity saved = settingsRepository.save(entity);
        return new SettingsDtos.SettingsDto(
                saved.getDarkMode(),
                saved.getNotifications(),
                saved.getAiDetection(),
                saved.getAutoBlock(),
                saved.getThreshold()
        );
    }

    @Transactional
    public SettingsDtos.SettingsDto resetSettings() {
        SystemSettingsEntity entity = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(new SystemSettingsEntity());

        entity.setDarkMode(false);
        entity.setNotifications(true);
        entity.setAiDetection(true);
        entity.setAutoBlock(true);
        entity.setThreshold(80);

        SystemSettingsEntity saved = settingsRepository.save(entity);
        return new SettingsDtos.SettingsDto(
                saved.getDarkMode(),
                saved.getNotifications(),
                saved.getAiDetection(),
                saved.getAutoBlock(),
                saved.getThreshold()
        );
    }

    public List<SettingsDtos.RuleSettingDto> getRules() {
        return fraudRuleRepository.findAll().stream()
                .map(r -> new SettingsDtos.RuleSettingDto(
                        r.getRuleKey(),
                        r.getRuleName(),
                        r.getDescription(),
                        r.getCategory(),
                        r.getThresholdValue(),
                        r.getWeight(),
                        r.getEnabled()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public List<SettingsDtos.RuleSettingDto> updateRules(List<SettingsDtos.RuleSettingDto> dtos) {
        if (dtos != null) {
            for (SettingsDtos.RuleSettingDto dto : dtos) {
                fraudRuleRepository.findByRuleKey(dto.getId()).ifPresent(rule -> {
                    if (dto.getWeight() != null) rule.setWeight(dto.getWeight());
                    if (dto.getThreshold() != null) rule.setThresholdValue(dto.getThreshold());
                    if (dto.getEnabled() != null) rule.setEnabled(dto.getEnabled());
                    rule.setUpdatedAt(java.time.LocalDateTime.now());
                    fraudRuleRepository.save(rule);
                });
            }
        }
        return getRules();
    }
}
