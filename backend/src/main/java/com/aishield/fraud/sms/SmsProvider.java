package com.aishield.fraud.sms;

public interface SmsProvider {

    SmsSendResult sendSms(String toPhone, String messageText);

    String getProviderName();

    boolean isConfigured();
}
