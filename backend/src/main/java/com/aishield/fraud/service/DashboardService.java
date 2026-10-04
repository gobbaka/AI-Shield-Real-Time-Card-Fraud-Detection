package com.aishield.fraud.service;

import com.aishield.fraud.dto.DashboardDtos;
import com.aishield.fraud.dto.TransactionDtos;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.repository.FraudAlertRepository;
import com.aishield.fraud.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionService transactionService;

    public DashboardService(TransactionRepository transactionRepository, 
                            FraudAlertRepository fraudAlertRepository, 
                            TransactionService transactionService) {
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionService = transactionService;
    }

    public DashboardDtos.DashboardSummaryDto getDashboardSummary() {
        DashboardDtos.DashboardSummaryDto summary = new DashboardDtos.DashboardSummaryDto();

        long totalTx = transactionRepository.count();
        long approvedCount = transactionRepository.countByStatus("Approved");
        long alertCount = fraudAlertRepository.findAll().stream()
                .filter(a -> !"Resolved".equalsIgnoreCase(a.getStatus()))
                .count();

        double safeRate = (totalTx > 0) ? Math.round(((double) approvedCount / totalTx) * 1000.0) / 10.0 : 98.8;
        Double avgRiskObj = transactionRepository.averageRiskScore();
        double avgRisk = (avgRiskObj != null) ? avgRiskObj : 0.0;
        double trustScore = (totalTx > 0) ? Math.max(1.0, Math.round(100.0 - avgRisk)) : 99.0;

        List<TransactionDtos.TransactionResponse> recent = transactionService.getRecentTransactions(5);

        summary.setTotalTransactions(totalTx);
        summary.setFraudAlertCount(alertCount);
        summary.setSafeTransactionRate(safeRate);
        summary.setTrustScore(trustScore);
        summary.setModelAccuracy(98);
        summary.setRecentTransactions(recent);

        return summary;
    }

    public DashboardDtos.SimulationResponse simulateTransaction(DashboardDtos.SimulationRequest request) {
        TransactionDtos.IngestTransactionRequest ingestReq = new TransactionDtos.IngestTransactionRequest();
        ingestReq.setAmount(request.getAmount() != null ? request.getAmount() : 99999.0);
        ingestReq.setLocation(request.getLocation() != null ? request.getLocation() : "Unknown/High-Risk IP");
        ingestReq.setCustomerName(request.getCustomerName() != null ? request.getCustomerName() : "Simulated Cardholder");
        ingestReq.setMerchantCategory(request.getMerchantCategory() != null ? request.getMerchantCategory() : "ELECTRONICS");
        ingestReq.setDeviceFingerprint("SIMULATED-DEV-99");

        TransactionDtos.TransactionResponse processed = transactionService.processTransaction(ingestReq);

        DashboardDtos.SimulationResponse response = new DashboardDtos.SimulationResponse();
        response.setTransactionId(processed.getId());
        response.setAmount(processed.getAmount());
        response.setCustomer(processed.getCustomer());
        response.setLocation(processed.getLocation());
        response.setRiskScore(processed.getRiskScore());
        response.setRiskLevel(processed.getRisk());
        response.setDecision(processed.getDecision());
        response.setStatus(processed.getStatus());
        response.setReasons(processed.getReasons());
        response.setMessage(String.format("Simulation completed for %s (₹%.0f): Fraud risk scored %d%% (%s) -> %s", 
                processed.getId(), processed.getAmount(), processed.getRiskScore(), processed.getRisk(), processed.getStatus()));

        return response;
    }
}
