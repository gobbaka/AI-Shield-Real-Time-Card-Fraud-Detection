package com.aishield.fraud.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class TwilioSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);

    private final RestTemplate restTemplate;

    @Value("${app.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String authToken;

    @Value("${app.sms.twilio.from-phone:}")
    private String fromPhone;

    public TwilioSmsProvider(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofMillis(2000))
                .setReadTimeout(java.time.Duration.ofMillis(2500))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank() && !accountSid.contains("dummy") &&
               authToken != null && !authToken.isBlank() && !authToken.contains("dummy") &&
               fromPhone != null && !fromPhone.isBlank();
    }

    @Override
    public String getProviderName() {
        return "TWILIO";
    }

    @Override
    public SmsSendResult sendSms(String toPhone, String messageText) {
        if (!isConfigured()) {
            return SmsSendResult.failure(getProviderName(), "Twilio SID/Auth Token/From Phone not configured.");
        }

        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid);

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(accountSid, authToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String requestBody = "To=" + URLEncoder.encode(toPhone, StandardCharsets.UTF_8) +
                                 "&From=" + URLEncoder.encode(fromPhone, StandardCharsets.UTF_8) +
                                 "&Body=" + URLEncoder.encode(messageText, StandardCharsets.UTF_8);

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String sid = response.getBody().get("sid") != null ? response.getBody().get("sid").toString() : "TW-" + System.currentTimeMillis();
                log.info("Twilio successfully dispatched SMS to {}. SID: {}", toPhone, sid);
                return SmsSendResult.success(getProviderName(), sid);
            } else {
                return SmsSendResult.failure(getProviderName(), "HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Twilio SMS dispatch failed: {}", e.getMessage());
            return SmsSendResult.failure(getProviderName(), e.getMessage());
        }
    }
}
