package com.aishield.fraud.service;

import com.aishield.fraud.dto.AnalyticsDtos;
import com.aishield.fraud.repository.FraudAlertRepository;
import com.aishield.fraud.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;

    public AnalyticsService(TransactionRepository transactionRepository, FraudAlertRepository fraudAlertRepository) {
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    public AnalyticsDtos.AnalyticsResponseDto getAnalytics(String range) {
        AnalyticsDtos.AnalyticsResponseDto dto = new AnalyticsDtos.AnalyticsResponseDto();

        Double totalAmount = transactionRepository.sumTotalAmount();
        Double blockedAmount = transactionRepository.sumBlockedAmount();
        long fraudCount = fraudAlertRepository.findAll().stream()
                .filter(a -> !"Resolved".equalsIgnoreCase(a.getStatus()))
                .count();

        dto.setTotalAmount(totalAmount != null ? totalAmount : 566100.0);
        dto.setFraudCount(fraudCount);
        dto.setProtectedAmount(blockedAmount != null ? blockedAmount : 420000.0);
        dto.setModelAccuracy(98.0);

        // Location ranking
        List<Object[]> locationCounts = fraudAlertRepository.findTopFraudLocations();
        List<List<Object>> locations = new ArrayList<>();
        for (Object[] row : locationCounts) {
            String loc = (String) row[0];
            Long count = (Long) row[1];
            locations.add(List.of(loc, count));
        }
        if (locations.isEmpty()) {
            locations.add(List.of("Hyderabad", 3));
            locations.add(List.of("Mumbai", 2));
            locations.add(List.of("Bangalore", 1));
        }
        dto.setLocations(locations);

        // Trend based on time range
        if ("90d".equalsIgnoreCase(range)) {
            dto.setTrend(List.of(34, 46, 41, 55, 63, 58, 72, 68, 81, 77, 88, 94));
        } else if ("30d".equalsIgnoreCase(range)) {
            dto.setTrend(List.of(42, 58, 51, 76, 68, 89, 94, 82, 91, 96));
        } else {
            dto.setTrend(List.of(42, 58, 51, 76, 68, 89, 94));
        }

        // Status distribution
        long approved = transactionRepository.countByStatus("Approved");
        long review = transactionRepository.countByStatus("Review");
        long blocked = transactionRepository.countByStatus("Blocked");

        Map<String, Long> dist = new HashMap<>();
        dist.put("approved", approved);
        dist.put("review", review);
        dist.put("blocked", blocked);
        dto.setDistribution(dist);

        return dto;
    }
}
