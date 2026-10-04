package com.aishield.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TravelNoticeDtos {

    public static class CreateTravelNoticeRequest {
        private String cardMasked;

        @NotBlank(message = "Destination country is required")
        private String destinationCountry;

        @NotBlank(message = "Destination city is required")
        private String destinationCity;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        public String getCardMasked() { return cardMasked; }
        public void setCardMasked(String cardMasked) { this.cardMasked = cardMasked; }
        public String getDestinationCountry() { return destinationCountry; }
        public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }
        public String getDestinationCity() { return destinationCity; }
        public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    public static class TravelNoticeResponse {
        private Long id;
        private String customerEmail;
        private String cardMasked;
        private String destinationCountry;
        private String destinationCity;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private LocalDateTime createdAt;
        private boolean isActiveNow;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public String getCardMasked() { return cardMasked; }
        public void setCardMasked(String cardMasked) { this.cardMasked = cardMasked; }
        public String getDestinationCountry() { return destinationCountry; }
        public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }
        public String getDestinationCity() { return destinationCity; }
        public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public boolean isActiveNow() { return isActiveNow; }
        public void setActiveNow(boolean activeNow) { isActiveNow = activeNow; }
    }
}