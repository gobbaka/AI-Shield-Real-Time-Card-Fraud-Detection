package com.aishield.fraud.sms;

public class SmsSendResult {

    private boolean success;
    private String status; // "SENT", "DELIVERED", "FAILED", "MOCK_DISPATCHED"
    private String providerName; // "FAST2SMS", "TWILIO", "MOCK"
    private String providerReference; // e.g. message ID / request ID
    private String errorMessage;

    private boolean simulation;
    private String simulatedOtp;

    public SmsSendResult() {}

    public SmsSendResult(boolean success, String status, String providerName, String providerReference, String errorMessage) {
        this.success = success;
        this.status = status;
        this.providerName = providerName;
        this.providerReference = providerReference;
        this.errorMessage = errorMessage;
    }

    public static SmsSendResult success(String providerName, String providerReference) {
        return new SmsSendResult(true, "SENT", providerName, providerReference, null);
    }

    public static SmsSendResult mock(String providerReference) {
        return new SmsSendResult(true, "MOCK_DISPATCHED", "MOCK", providerReference, "Dispatched via development mock provider");
    }

    public static SmsSendResult failure(String providerName, String errorMessage) {
        return new SmsSendResult(false, "FAILED", providerName, null, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}


