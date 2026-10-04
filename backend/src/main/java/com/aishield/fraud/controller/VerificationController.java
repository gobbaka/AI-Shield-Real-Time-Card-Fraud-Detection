package com.aishield.fraud.controller;

import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.dto.VerificationDtos;
import com.aishield.fraud.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/verifications")
@CrossOrigin(origins = "*")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<VerificationDtos.VerificationRequestResponse>>> getPendingVerifications(
            @RequestParam(required = false) String email,
            @AuthenticationPrincipal UserDetails userDetails) {
        String targetEmail = (email != null && !email.isBlank()) ? email :
                (userDetails != null ? userDetails.getUsername() : null);

        List<VerificationDtos.VerificationRequestResponse> list = (targetEmail != null) ?
                verificationService.getPendingForCustomer(targetEmail) :
                verificationService.getAllPendingVerifications();

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<VerificationDtos.VerificationRequestResponse>> getVerificationById(
            @PathVariable String requestId) {
        VerificationDtos.VerificationRequestResponse response = verificationService.getVerificationById(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{requestId}/send-sms")
    public ResponseEntity<ApiResponse<String>> sendSmsToPhone(
            @PathVariable String requestId,
            @RequestBody(required = false) VerificationDtos.SendSmsRequest request) {
        String targetPhone = (request != null && request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank())
                ? request.getPhoneNumber() : "+91 98111 22334";

        boolean sent = verificationService.resendSmsToPhone(requestId, targetPhone);
        return ResponseEntity.ok(ApiResponse.success("Bank fraud alert SMS dispatched to " + targetPhone, sent ? "SENT" : "FAILED"));
    }

    @PostMapping("/{requestId}/step-up")
    public ResponseEntity<ApiResponse<VerificationDtos.VerificationActionResponse>> confirmStepUp(
            @PathVariable String requestId,
            @Valid @RequestBody VerificationDtos.StepUpAuthRequest request) {
        VerificationDtos.VerificationActionResponse response = verificationService.verifyStepUp(requestId, request);
        return ResponseEntity.ok(ApiResponse.success("Step-up verification completed", response));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<VerificationDtos.VerificationActionResponse>> rejectVerification(
            @PathVariable String requestId,
            @RequestBody(required = false) VerificationDtos.RejectVerificationRequest request) {
        VerificationDtos.VerificationActionResponse response = verificationService.rejectVerification(requestId, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction reported as unauthorized fraud", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<VerificationDtos.VerificationHistoryResponse>>> getVerificationHistory(
            @RequestParam(required = false) String email,
            @AuthenticationPrincipal UserDetails userDetails) {
        String targetEmail = (email != null && !email.isBlank()) ? email :
                (userDetails != null ? userDetails.getUsername() : null);
        List<VerificationDtos.VerificationHistoryResponse> list = verificationService.getVerificationHistory(targetEmail);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
