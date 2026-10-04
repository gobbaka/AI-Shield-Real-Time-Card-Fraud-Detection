package com.aishield.fraud.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSmsProvider.class);

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public SmsSendResult sendSms(String toPhone, String messageText) {
        String mockId = "MOCK-SMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sanitized = messageText != null ? messageText.replaceAll("\\b\\d{6}\\b", "******") : "";

        log.info("[AI SHIELD MOCK SMS] Mock dispatch to {}: {}", toPhone, sanitized);
        return SmsSendResult.mock(mockId);
    }
}
