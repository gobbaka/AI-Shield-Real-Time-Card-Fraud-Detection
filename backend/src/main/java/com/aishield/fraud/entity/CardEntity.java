package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
public class CardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    @Convert(converter = com.aishield.fraud.config.CardPanEncryptor.class)
    private String cardNumberMasked; // e.g. "**** **** **** 4721"

    @Column(nullable = false)
    private String cardLast4;

    private String cardType; // "VISA", "MASTERCARD", "RUPAY"

    @Column(nullable = false)
    private String status; // "ACTIVE", "BLOCKED", "REVIEW"

    private Double dailyLimit = 100000.0;
    private Double monthlyLimit = 500000.0;

    private LocalDateTime createdAt = LocalDateTime.now();

    public CardEntity() {}

    public CardEntity(UserEntity user, String cardNumberMasked, String cardLast4, String cardType, String status, Double dailyLimit) {
        this.user = user;
        this.cardNumberMasked = cardNumberMasked;
        this.cardLast4 = cardLast4;
        this.cardType = cardType;
        this.status = status;
        this.dailyLimit = dailyLimit;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public String getCardNumberMasked() { return cardNumberMasked; }
    public void setCardNumberMasked(String cardNumberMasked) { this.cardNumberMasked = cardNumberMasked; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Double dailyLimit) { this.dailyLimit = dailyLimit; }

    public Double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(Double monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
