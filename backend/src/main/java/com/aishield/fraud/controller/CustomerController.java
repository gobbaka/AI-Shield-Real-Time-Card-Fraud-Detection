package com.aishield.fraud.controller;

import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.dto.CustomerDtos;
import com.aishield.fraud.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerDtos.CustomerResponse>>> getAllCustomers() {
        List<CustomerDtos.CustomerResponse> list = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/cards")
    public ResponseEntity<ApiResponse<List<CustomerDtos.CardResponse>>> getUserCards(
            @RequestParam(required = false, defaultValue = "") String email) {
        List<CustomerDtos.CardResponse> list = customerService.getCardsByUserEmail(email);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/cards/{cardId}/toggle-lock")
    public ResponseEntity<ApiResponse<CustomerDtos.CardResponse>> toggleCardLock(@PathVariable Long cardId) {
        CustomerDtos.CardResponse updated = customerService.toggleCardLock(cardId);
        return ResponseEntity.ok(ApiResponse.success("Card lock status updated", updated));
    }

    @PutMapping("/cards/{cardId}/limit")
    public ResponseEntity<ApiResponse<CustomerDtos.CardResponse>> updateCardLimit(
            @PathVariable Long cardId,
            @RequestBody CustomerDtos.UpdateLimitRequest request) {
        CustomerDtos.CardResponse updated = customerService.updateDailyLimit(cardId, request.getDailyLimit());
        return ResponseEntity.ok(ApiResponse.success("Card daily limit updated", updated));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<com.aishield.fraud.dto.TransactionDtos.TransactionResponse>>> getCustomerTransactions(
            @RequestParam(required = false, defaultValue = "") String email) {
        List<com.aishield.fraud.dto.TransactionDtos.TransactionResponse> list = customerService.getTransactionsByUserEmail(email);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/disputes")
    public ResponseEntity<ApiResponse<CustomerDtos.DisputeResponse>> fileDispute(
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestBody CustomerDtos.DisputeRequest request) {
        CustomerDtos.DisputeResponse response = customerService.disputeTransaction(email, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction dispute recorded successfully", response));
    }

    // ==========================================
    // TRAVEL NOTICE / TRAVEL MODE ENDPOINTS
    // ==========================================
    @PostMapping("/travel-notices")
    public ResponseEntity<ApiResponse<com.aishield.fraud.dto.TravelNoticeDtos.TravelNoticeResponse>> createTravelNotice(
            @RequestParam(required = false) String email,
            @RequestBody com.aishield.fraud.dto.TravelNoticeDtos.CreateTravelNoticeRequest request) {
        com.aishield.fraud.dto.TravelNoticeDtos.TravelNoticeResponse resp = customerService.createTravelNotice(email, request);
        return ResponseEntity.ok(ApiResponse.success("Travel Mode notice activated. Geo-velocity anomalies will be suppressed for this destination.", resp));
    }

    @GetMapping("/travel-notices")
    public ResponseEntity<ApiResponse<List<com.aishield.fraud.dto.TravelNoticeDtos.TravelNoticeResponse>>> getTravelNotices(
            @RequestParam(required = false, defaultValue = "priya@example.com") String email) {
        List<com.aishield.fraud.dto.TravelNoticeDtos.TravelNoticeResponse> list = customerService.getTravelNoticesByUserEmail(email);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @DeleteMapping("/travel-notices/{noticeId}")
    public ResponseEntity<ApiResponse<String>> cancelTravelNotice(@PathVariable Long noticeId) {
        boolean cancelled = customerService.cancelTravelNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.success(cancelled ? "Travel Notice cancelled" : "Travel Notice not found", cancelled ? "SUCCESS" : "NOT_FOUND"));
    }
}
