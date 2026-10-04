package com.aishield.fraud.dto;

public class SettingsDtos {

    public static class SettingsDto {
        private Boolean darkMode;
        private Boolean notifications;
        private Boolean aiDetection;
        private Boolean autoBlock;
        private Integer threshold;

        public SettingsDto() {}

        public SettingsDto(Boolean darkMode, Boolean notifications, Boolean aiDetection, Boolean autoBlock, Integer threshold) {
            this.darkMode = darkMode;
            this.notifications = notifications;
            this.aiDetection = aiDetection;
            this.autoBlock = autoBlock;
            this.threshold = threshold;
        }

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
    }

    public static class RuleSettingDto {
        private String id;
        private String name;
        private String description;
        private String category;
        private Double threshold;
        private Double weight;
        private Boolean enabled;

        public RuleSettingDto() {}

        public RuleSettingDto(String id, String name, String description, String category, Double threshold, Double weight, Boolean enabled) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.threshold = threshold;
            this.weight = weight;
            this.enabled = enabled;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Double getThreshold() { return threshold; }
        public void setThreshold(Double threshold) { this.threshold = threshold; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }
}
