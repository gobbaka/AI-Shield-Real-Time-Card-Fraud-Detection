package com.aishield.fraud.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsDtos {

    public static class AnalyticsResponseDto {
        private Double totalAmount;
        private Long fraudCount;
        private Double modelAccuracy;
        private Double protectedAmount;
        private List<List<Object>> locations = new ArrayList<>();
        private List<Integer> trend = new ArrayList<>();
        private Map<String, Long> distribution = new HashMap<>();

        public AnalyticsResponseDto() {}

        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
        public Long getFraudCount() { return fraudCount; }
        public void setFraudCount(Long fraudCount) { this.fraudCount = fraudCount; }
        public Double getModelAccuracy() { return modelAccuracy; }
        public void setModelAccuracy(Double modelAccuracy) { this.modelAccuracy = modelAccuracy; }
        public Double getProtectedAmount() { return protectedAmount; }
        public void setProtectedAmount(Double protectedAmount) { this.protectedAmount = protectedAmount; }
        public List<List<Object>> getLocations() { return locations; }
        public void setLocations(List<List<Object>> locations) { this.locations = locations; }
        public List<Integer> getTrend() { return trend; }
        public void setTrend(List<Integer> trend) { this.trend = trend; }
        public Map<String, Long> getDistribution() { return distribution; }
        public void setDistribution(Map<String, Long> distribution) { this.distribution = distribution; }
    }
}
