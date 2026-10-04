package com.aishield.fraud.controller;

import com.aishield.fraud.dto.AnalyticsDtos;
import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AnalyticsDtos.AnalyticsResponseDto>> getAnalytics(
            @RequestParam(defaultValue = "7d") String range) {
        AnalyticsDtos.AnalyticsResponseDto analytics = analyticsService.getAnalytics(range);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
