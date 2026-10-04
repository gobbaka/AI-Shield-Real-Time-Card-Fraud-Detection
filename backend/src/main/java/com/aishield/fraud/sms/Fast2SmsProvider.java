package com.aishield.fraud.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class Fast2SmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(Fast2SmsProvider.class);

    private final RestTemplate restTemplate;

    @Value("${app.sms.fast2sms.api-key:}")
    private String apiKey;

    @Value("${app.sms.fast2sms.otp-id:}")
    private String otpId;

    @Value("${app.sms.fast2sms.route:auto}")
    private String routeConfig;

    public Fast2SmsProvider(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofMillis(5000))
                .setReadTimeout(java.time.Duration.ofMillis(8000))
                .build();
    }

    public String getEffectiveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        String sysProp = System.getProperty("FAST2SMS_API_KEY");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String env = System.getenv("FAST2SMS_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return null;
    }

    public String getEffectiveOtpId() {
        if (otpId != null && !otpId.isBlank()) {
            return otpId.trim();
        }
        String sysProp = System.getProperty("FAST2SMS_OTP_ID");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String env = System.getenv("FAST2SMS_OTP_ID");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return null;
    }

    public String getEffectiveRoute() {
        if (routeConfig != null && !routeConfig.isBlank()) {
            return routeConfig.trim();
        }
        String sysProp = System.getProperty("FAST2SMS_ROUTE");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String env = System.getenv("FAST2SMS_ROUTE");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return "auto";
    }

    @Override
    public boolean isConfigured() {
        String key = getEffectiveApiKey();
        return key != null && !key.isBlank() && !key.contains("dummy") && !key.contains("your_");
    }

    @Override
    public String getProviderName() {
        return "FAST2SMS";
    }

    @Override
    public SmsSendResult sendSms(String toPhone, String messageText) {
        if (!isConfigured()) {
            return SmsSendResult.failure(getProviderName(), "Fast2SMS API Key not configured in environment / .env file.");
        }

        String effectiveKey = getEffectiveApiKey();
        String effectiveOtpId = getEffectiveOtpId();
        String effectiveRoute = getEffectiveRoute();

        try {
            String numbersOnly = toPhone.replaceAll("[^0-9]", "");
            if (numbersOnly.length() > 10) {
                numbersOnly = numbersOnly.substring(numbersOnly.length() - 10);
            }

            if (numbersOnly.length() != 10) {
                return SmsSendResult.failure(getProviderName(), "Invalid Indian mobile number: " + toPhone + " (must be 10 digits)");
            }

            String extractedOtp = extractOtp(messageText);

            // PRIORITY 1: Modern Fast2SMS Smart OTP API (POST https://www.fast2sms.com/dev/otp/send)
            // If FAST2SMS_OTP_ID is set OR route is explicitly configured for smart OTP
            if (effectiveOtpId != null && !effectiveOtpId.isBlank()) {
                return sendViaSmartOtpApi(numbersOnly, extractedOtp, effectiveKey, effectiveOtpId);
            }

            // PRIORITY 2: If route is explicitly 'q' (Quick SMS via bulkV2)
            if ("q".equalsIgnoreCase(effectiveRoute)) {
                return sendViaQuickSmsRoute(numbersOnly, messageText, effectiveKey);
            }

            // PRIORITY 3: If route is explicitly 'otp' (OTP template via bulkV2)
            if ("otp".equalsIgnoreCase(effectiveRoute) && extractedOtp != null) {
                return sendViaBulkV2OtpRoute(numbersOnly, extractedOtp, effectiveKey);
            }

            // Default / Auto when no OTP ID is configured yet:
            // Attempt Smart OTP endpoint directly to check if account has a default or validate requirement
            return sendViaSmartOtpApi(numbersOnly, extractedOtp, effectiveKey, "");

        } catch (HttpStatusCodeException e) {
            String respBody = e.getResponseBodyAsString();
            String errorMsg = parseErrorMessage(respBody, e.getMessage());
            log.error("[REAL SMS ERROR] Fast2SMS HTTP {}: {}", e.getStatusCode(), respBody);
            return SmsSendResult.failure(getProviderName(), "Fast2SMS Gateway Error (HTTP " + e.getStatusCode().value() + "): " + errorMsg);
        } catch (Exception e) {
            log.error("[REAL SMS ERROR] Fast2SMS dispatch failed: {}", e.getMessage());
            return SmsSendResult.failure(getProviderName(), "SMS delivery failed: " + e.getMessage());
        }
    }

    /**
     * Official Fast2SMS Smart OTP API
     * POST https://www.fast2sms.com/dev/otp/send
     */
    private SmsSendResult sendViaSmartOtpApi(String mobile, String otp, String apiKey, String otpId) {
        if (otpId == null || otpId.isBlank()) {
            return SmsSendResult.failure(
                    getProviderName(),
                    "Fast2SMS Smart OTP requires 'FAST2SMS_OTP_ID'. Please create an OTP Template in your Fast2SMS dashboard (Smart OTP section) and add FAST2SMS_OTP_ID to your .env file."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("otp_id", otpId);
        body.put("mobile", mobile);
        if (otp != null && !otp.isBlank()) {
            body.put("otp", otp);
        }
        body.put("otp_expiry", 5);
        body.put("otp_length", 6);

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.fast2sms.com/dev/otp/send",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map respBody = response.getBody();
                boolean isSuccess = Boolean.TRUE.equals(respBody.get("return"));
                String requestId = respBody.get("request_id") != null ? respBody.get("request_id").toString() : "F2S-" + System.currentTimeMillis();
                if (isSuccess) {
                    log.info("[REAL SMS DELIVERY] Fast2SMS Smart OTP successfully sent to {}. Request ID: {}", mobile, requestId);
                    return SmsSendResult.success(getProviderName(), requestId);
                } else {
                    String message = respBody.get("message") != null ? respBody.get("message").toString() : "Gateway rejection";
                    log.error("[REAL SMS ERROR] Fast2SMS Smart OTP returned failure: {}", message);
                    return SmsSendResult.failure(getProviderName(), "Fast2SMS Smart OTP rejected: " + message);
                }
            } else {
                return SmsSendResult.failure(getProviderName(), "Fast2SMS returned HTTP " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            String respBody = e.getResponseBodyAsString();
            String errorMsg = parseErrorMessage(respBody, e.getMessage());
            log.error("[REAL SMS ERROR] Fast2SMS Smart OTP HTTP {}: {}", e.getStatusCode(), respBody);
            return SmsSendResult.failure(getProviderName(), "Fast2SMS Smart OTP Error (HTTP " + e.getStatusCode().value() + "): " + errorMsg);
        }
    }

    /**
     * Fast2SMS bulkV2 Route 'q' (Quick SMS)
     */
    private SmsSendResult sendViaQuickSmsRoute(String mobile, String messageText, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("route", "q");
        body.put("message", messageText);
        body.put("numbers", mobile);

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.fast2sms.com/dev/bulkV2",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map respBody = response.getBody();
                boolean isSuccess = Boolean.TRUE.equals(respBody.get("return"));
                String requestId = respBody.get("request_id") != null ? respBody.get("request_id").toString() : "F2S-" + System.currentTimeMillis();
                if (isSuccess) {
                    log.info("[REAL SMS DELIVERY] Fast2SMS Quick SMS successfully sent to {}. Request ID: {}", mobile, requestId);
                    return SmsSendResult.success(getProviderName(), requestId);
                } else {
                    String message = respBody.get("message") != null ? respBody.get("message").toString() : "Gateway rejection";
                    log.error("[REAL SMS ERROR] Fast2SMS Quick SMS returned failure: {}", message);
                    return SmsSendResult.failure(getProviderName(), "Fast2SMS Quick SMS rejected: " + message);
                }
            } else {
                return SmsSendResult.failure(getProviderName(), "Fast2SMS returned HTTP " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            String respBody = e.getResponseBodyAsString();
            String errorMsg = parseErrorMessage(respBody, e.getMessage());
            log.error("[REAL SMS ERROR] Fast2SMS Quick SMS HTTP {}: {}", e.getStatusCode(), respBody);
            return SmsSendResult.failure(getProviderName(), "Fast2SMS Quick SMS Error (HTTP " + e.getStatusCode().value() + "): " + errorMsg);
        }
    }

    /**
     * Fast2SMS bulkV2 Route 'otp' (Requires DLT / Website verification)
     */
    private SmsSendResult sendViaBulkV2OtpRoute(String mobile, String otp, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("route", "otp");
        body.put("variables_values", otp);
        body.put("numbers", mobile);

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.fast2sms.com/dev/bulkV2",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map respBody = response.getBody();
                boolean isSuccess = Boolean.TRUE.equals(respBody.get("return"));
                String requestId = respBody.get("request_id") != null ? respBody.get("request_id").toString() : "F2S-" + System.currentTimeMillis();
                if (isSuccess) {
                    log.info("[REAL SMS DELIVERY] Fast2SMS OTP route successfully sent to {}. Request ID: {}", mobile, requestId);
                    return SmsSendResult.success(getProviderName(), requestId);
                } else {
                    String message = respBody.get("message") != null ? respBody.get("message").toString() : "Gateway rejection";
                    return SmsSendResult.failure(getProviderName(), "Fast2SMS OTP route rejected: " + message);
                }
            } else {
                return SmsSendResult.failure(getProviderName(), "Fast2SMS returned HTTP " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            String respBody = e.getResponseBodyAsString();
            String errorMsg = parseErrorMessage(respBody, e.getMessage());
            log.error("[REAL SMS ERROR] Fast2SMS OTP Route HTTP {}: {}", e.getStatusCode(), respBody);
            return SmsSendResult.failure(getProviderName(), "Fast2SMS OTP Route Error (HTTP " + e.getStatusCode().value() + "): " + errorMsg);
        }
    }

    private String parseErrorMessage(String respBody, String defaultMsg) {
        if (respBody == null || respBody.isBlank()) return defaultMsg;
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"").matcher(respBody);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception ignored) {}
        return respBody;
    }

    private String extractOtp(String messageText) {
        if (messageText == null) return null;
        java.util.regex.Matcher otpMatcher = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(messageText);
        if (otpMatcher.find()) {
            return otpMatcher.group(1);
        }
        return null;
    }
}
