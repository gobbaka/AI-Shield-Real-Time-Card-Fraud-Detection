package com.aishield.fraud.controller;

import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.dto.SettingsDtos;
import com.aishield.fraud.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@CrossOrigin(origins = "*")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SettingsDtos.SettingsDto>> getSettings() {
        SettingsDtos.SettingsDto settings = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SettingsDtos.SettingsDto>> updateSettings(
            @RequestBody SettingsDtos.SettingsDto dto) {
        SettingsDtos.SettingsDto updated = settingsService.updateSettings(dto);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", updated));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<SettingsDtos.SettingsDto>> resetSettings() {
        SettingsDtos.SettingsDto reset = settingsService.resetSettings();
        return ResponseEntity.ok(ApiResponse.success("Settings reset to defaults", reset));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<java.util.List<SettingsDtos.RuleSettingDto>>> getRules() {
        java.util.List<SettingsDtos.RuleSettingDto> rules = settingsService.getRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @PutMapping("/rules")
    public ResponseEntity<ApiResponse<java.util.List<SettingsDtos.RuleSettingDto>>> updateRules(
            @RequestBody java.util.List<SettingsDtos.RuleSettingDto> dtos) {
        java.util.List<SettingsDtos.RuleSettingDto> updated = settingsService.updateRules(dtos);
        return ResponseEntity.ok(ApiResponse.success("Fraud rules and weight configurations updated successfully", updated));
    }
}
