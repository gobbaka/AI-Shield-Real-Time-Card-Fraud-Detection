package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; // "ADMIN", "FRAUD_ANALYST", "CUSTOMER"

    private String phone;

    @Column(nullable = false)
    private Boolean mobileVerified = false;

    private LocalDateTime mobileVerifiedAt;

    private String location;

    @Column(length = 1000)
    private String bio;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardEntity> cards = new ArrayList<>();

    public UserEntity() {}

    public UserEntity(String name, String email, String passwordHash, String role, String phone, String location, String bio) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.phone = phone;
        this.location = location;
        this.bio = bio;
        this.mobileVerified = (phone != null && !phone.isBlank());
        if (this.mobileVerified) {
            this.mobileVerifiedAt = LocalDateTime.now();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getMobileVerified() { return mobileVerified != null && mobileVerified; }
    public void setMobileVerified(Boolean mobileVerified) { this.mobileVerified = mobileVerified; }

    public LocalDateTime getMobileVerifiedAt() { return mobileVerifiedAt; }
    public void setMobileVerifiedAt(LocalDateTime mobileVerifiedAt) { this.mobileVerifiedAt = mobileVerifiedAt; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<CardEntity> getCards() { return cards; }
    public void setCards(List<CardEntity> cards) { this.cards = cards; }
}
