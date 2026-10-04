package com.aishield.fraud.controller;

import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.dto.DashboardDtos;
import com.aishield.fraud.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardDtos.DashboardSummaryDto>> getSummary() {
        DashboardDtos.DashboardSummaryDto summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<DashboardDtos.SimulationResponse>> simulateTransaction(
            @RequestBody DashboardDtos.SimulationRequest request) {
        DashboardDtos.SimulationResponse response = dashboardService.simulateTransaction(request);
        return ResponseEntity.ok(ApiResponse.success("Simulation executed", response));
    }
}
