package com.aishield.fraud.controller;

import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.entity.FraudAuditLogEntity;
import com.aishield.fraud.service.AuditLoggingService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLoggingService auditLoggingService;

    public AuditLogController(AuditLoggingService auditLoggingService) {
        this.auditLoggingService = auditLoggingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ApiResponse<Page<FraudAuditLogEntity>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<FraudAuditLogEntity> logs = auditLoggingService.getRecentAuditLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/transaction/{txRef}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ApiResponse<List<FraudAuditLogEntity>>> getAuditLogsByTransaction(
            @PathVariable String txRef) {
        List<FraudAuditLogEntity> logs = auditLoggingService.getAuditLogsForTransaction(txRef);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
