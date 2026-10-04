package com.aishield.fraud.controller;

import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.dto.TransactionDtos;
import com.aishield.fraud.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionDtos.IngestTransactionRequest request) {
        TransactionDtos.TransactionResponse response = transactionService.processTransaction(request);
        return ResponseEntity.ok(ApiResponse.success("Transaction evaluated and processed", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionDtos.TransactionResponse>>> getAllTransactions() {
        List<TransactionDtos.TransactionResponse> list = transactionService.getAllTransactions();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<TransactionDtos.TransactionResponse>>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        List<TransactionDtos.TransactionResponse> list = transactionService.getRecentTransactions(limit);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{ref}")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> getTransactionByRef(
            @PathVariable String ref) {
        TransactionDtos.TransactionResponse response = transactionService.getTransactionByRef(ref);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{ref}/block")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> blockTransaction(
            @PathVariable String ref) {
        TransactionDtos.TransactionResponse response = transactionService.blockTransaction(ref);
        return ResponseEntity.ok(ApiResponse.success("Transaction blocked successfully", response));
    }
}
