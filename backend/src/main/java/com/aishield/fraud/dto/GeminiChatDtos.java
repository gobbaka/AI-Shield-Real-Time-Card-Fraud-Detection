package com.aishield.fraud.dto;

import java.util.List;

public class GeminiChatDtos {

    public static class ChatTurn {
        private String role;
        private String text;

        public ChatTurn() {}

        public ChatTurn(String role, String text) {
            this.role = role;
            this.text = text;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class ChatRequest {
        private String message;
        private List<ChatTurn> history;
        private String apiKey;

        public ChatRequest() {}

        public ChatRequest(String message, List<ChatTurn> history, String apiKey) {
            this.message = message;
            this.history = history;
            this.apiKey = apiKey;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<ChatTurn> getHistory() { return history; }
        public void setHistory(List<ChatTurn> history) { this.history = history; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class ChatResponse {
        private String reply;
        private String intent;
        private boolean projectRelated;
        private String actionRoute;
        private String actionLabel;
        private List<String> quickReplies;

        public ChatResponse() {}

        public ChatResponse(String reply, String intent, boolean projectRelated, String actionRoute, String actionLabel, List<String> quickReplies) {
            this.reply = reply;
            this.intent = intent;
            this.projectRelated = projectRelated;
            this.actionRoute = actionRoute;
            this.actionLabel = actionLabel;
            this.quickReplies = quickReplies;
        }

        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public boolean isProjectRelated() { return projectRelated; }
        public void setProjectRelated(boolean projectRelated) { this.projectRelated = projectRelated; }
        public String getActionRoute() { return actionRoute; }
        public void setActionRoute(String actionRoute) { this.actionRoute = actionRoute; }
        public String getActionLabel() { return actionLabel; }
        public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
        public List<String> getQuickReplies() { return quickReplies; }
        public void setQuickReplies(List<String> quickReplies) { this.quickReplies = quickReplies; }
    }
}
