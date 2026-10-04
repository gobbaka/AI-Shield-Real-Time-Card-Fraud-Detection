package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_notices")
public class TravelNoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String cardMasked;

    @Column(nullable = false)
    private String destinationCountry;

    @Column(nullable = false)
    private String destinationCity;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String status = "ACTIVE"; // "ACTIVE", "EXPIRED", "CANCELLED"

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TravelNoticeEntity() {}

    public TravelNoticeEntity(UserEntity user, String customerEmail, String cardMasked, 
                              String destinationCountry, String destinationCity, 
                              LocalDate startDate, LocalDate endDate) {
        this.user = user;
        this.customerEmail = customerEmail;
        this.cardMasked = cardMasked;
        this.destinationCountry = destinationCountry;
        this.destinationCity = destinationCity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

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
}