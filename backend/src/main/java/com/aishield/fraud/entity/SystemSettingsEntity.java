package com.aishield.fraud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings")
public class SystemSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Boolean darkMode = false;

    @Column(nullable = false)
    private Boolean notifications = true;

    @Column(nullable = false)
    private Boolean aiDetection = true;

    @Column(nullable = false)
    private Boolean autoBlock = false;

    @Column(nullable = false)
    private Integer threshold = 80;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public SystemSettingsEntity() {}

    public SystemSettingsEntity(Boolean darkMode, Boolean notifications, Boolean aiDetection, Boolean autoBlock, Integer threshold) {
        this.darkMode = darkMode;
        this.notifications = notifications;
        this.aiDetection = aiDetection;
        this.autoBlock = autoBlock;
        this.threshold = threshold;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Boolean getDarkMode() { return darkMode; }
    public void setDarkMode(Boolean darkMode) { this.darkMode = darkMode; }

    public Boolean getNotifications() { return notifications; }
    public void setNotifications(Boolean notifications) { this.notifications = notifications; }

    public Boolean getAiDetection() { return aiDetection; }
    public void setAiDetection(Boolean aiDetection) { this.aiDetection = aiDetection; }

    public Boolean getAutoBlock() { return autoBlock; }
    public void setAutoBlock(Boolean autoBlock) { this.autoBlock = autoBlock; }

    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
