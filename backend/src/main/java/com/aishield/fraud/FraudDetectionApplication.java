package com.aishield.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class FraudDetectionApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(FraudDetectionApplication.class, args);
    }

    private static void loadDotEnv() {
        String[] possiblePaths = { ".env", "../.env", "backend/.env" };
        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                try {
                    List<String> lines = Files.readAllLines(f.toPath());
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                            int eqIdx = trimmed.indexOf('=');
                            String key = trimmed.substring(0, eqIdx).trim();
                            String val = trimmed.substring(eqIdx + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            // Strip any trailing comments (e.g. key=value # comment)
                            if (val.contains("#") && !val.startsWith("\"") && !val.startsWith("'")) {
                                val = val.substring(0, val.indexOf('#')).trim();
                            }
                            String existingEnv = System.getenv(key);
                            String existingProp = System.getProperty(key);
                            boolean hasEnv = existingEnv != null && !existingEnv.isBlank();
                            boolean hasProp = existingProp != null && !existingProp.isBlank();

                            if (!hasProp && !hasEnv && !val.isBlank()) {
                                System.setProperty(key, val);
                                if ("FAST2SMS_API_KEY".equals(key)) {
                                    System.setProperty("app.sms.fast2sms.api-key", val);
                                } else if ("FAST2SMS_ROUTE".equals(key)) {
                                    System.setProperty("app.sms.fast2sms.route", val);
                                } else if ("TWILIO_ACCOUNT_SID".equals(key)) {
                                    System.setProperty("app.sms.twilio.account-sid", val);
                                } else if ("TWILIO_AUTH_TOKEN".equals(key)) {
                                    System.setProperty("app.sms.twilio.auth-token", val);
                                } else if ("TWILIO_FROM_PHONE".equals(key) || "TWILIO_PHONE_NUMBER".equals(key)) {
                                    System.setProperty("app.sms.twilio.from-phone", val);
                                } else if ("SMS_PROVIDER".equals(key)) {
                                    System.setProperty("app.sms.provider", val);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                break;
            }
        }
    }
}
