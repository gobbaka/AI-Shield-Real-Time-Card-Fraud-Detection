package com.aishield.fraud.controller;

import com.aishield.fraud.dto.AlertDtos;
import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.service.FraudAlertService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@CrossOrigin(origins = "*")
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    public FraudAlertController(FraudAlertService fraudAlertService) {
        this.fraudAlertService = fraudAlertService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertDtos.AlertResponse>>> getAllAlerts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        List<AlertDtos.AlertResponse> list = fraudAlertService.getAllAlerts(search, status);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{ref}")
    public ResponseEntity<ApiResponse<AlertDtos.AlertResponse>> getAlertByRef(@PathVariable String ref) {
        AlertDtos.AlertResponse response = fraudAlertService.getAlertByRef(ref);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{ref}/status")
    public ResponseEntity<ApiResponse<AlertDtos.AlertResponse>> updateStatus(
            @PathVariable String ref,
            @Valid @RequestBody AlertDtos.UpdateAlertStatusRequest request) {
        AlertDtos.AlertResponse response = fraudAlertService.updateAlertStatus(ref, request);
        return ResponseEntity.ok(ApiResponse.success("Alert status updated to " + request.getStatus(), response));
    }
}
