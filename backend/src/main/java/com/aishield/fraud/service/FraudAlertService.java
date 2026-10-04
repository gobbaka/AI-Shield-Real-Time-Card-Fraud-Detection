package com.aishield.fraud.service;

import com.aishield.fraud.dto.AlertDtos;
import com.aishield.fraud.entity.FraudAlertEntity;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.exception.ResourceNotFoundException;
import com.aishield.fraud.repository.FraudAlertRepository;
import com.aishield.fraud.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;

    public FraudAlertService(FraudAlertRepository fraudAlertRepository, TransactionRepository transactionRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<AlertDtos.AlertResponse> getAllAlerts(String query, String status) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        String cleanStatus = (status != null && !status.equalsIgnoreCase("All")) ? status.trim() : null;

        return fraudAlertRepository.searchAlerts(cleanQuery, cleanStatus)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AlertDtos.AlertResponse getAlertByRef(String alertRef) {
        FraudAlertEntity entity = fraudAlertRepository.findByAlertRef(alertRef)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud Alert not found with ref: " + alertRef));
        return mapToResponse(entity);
    }

    @Transactional
    public AlertDtos.AlertResponse updateAlertStatus(String alertRef, AlertDtos.UpdateAlertStatusRequest request) {
        FraudAlertEntity alert = fraudAlertRepository.findByAlertRef(alertRef)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud Alert not found with ref: " + alertRef));

        alert.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            alert.setInvestigatorNotes(request.getNotes());
        }

        if ("Resolved".equalsIgnoreCase(request.getStatus())) {
            alert.setResolvedAt(LocalDateTime.now());
        }

        // Sync corresponding transaction status
        if (alert.getTransactionRef() != null) {
            transactionRepository.findByTransactionRef(alert.getTransactionRef()).ifPresent(tx -> {
                if ("Blocked".equalsIgnoreCase(request.getStatus())) {
                    tx.setStatus("Blocked");
                    tx.setDecision("DECLINE");
                } else if ("Resolved".equalsIgnoreCase(request.getStatus())) {
                    tx.setStatus("Approved");
                    tx.setDecision("APPROVE");
                }
                transactionRepository.save(tx);
            });
        }

        FraudAlertEntity saved = fraudAlertRepository.save(alert);
        return mapToResponse(saved);
    }

    public AlertDtos.AlertResponse mapToResponse(FraudAlertEntity entity) {
        AlertDtos.AlertResponse res = new AlertDtos.AlertResponse();
        res.setId(entity.getAlertRef());
        res.setTransactionId(entity.getTransactionRef() != null ? entity.getTransactionRef() : "");
        res.setCustomer(entity.getCustomerName());
        res.setAmount(entity.getAmount());
        res.setRiskScore(entity.getRiskScore());
        res.setLocation(entity.getLocation());
        res.setStatus(entity.getStatus());
        res.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : "");
        res.setTriggerReasons(entity.getTriggerReasons());
        res.setAssignedInvestigator(entity.getAssignedInvestigator());
        res.setInvestigatorNotes(entity.getInvestigatorNotes());
        return res;
    }
}
