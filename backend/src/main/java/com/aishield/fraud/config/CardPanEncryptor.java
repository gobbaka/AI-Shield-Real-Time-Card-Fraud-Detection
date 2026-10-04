package com.aishield.fraud.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class CardPanEncryptor implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(CardPanEncryptor.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    // 256-bit Vault Encryption Key (PCI-DSS Application-Level Vault Key)
    private static final byte[] DEFAULT_KEY_BYTES = "AiShieldCardVaultSecretKey2026!#".getBytes(StandardCharsets.UTF_8);
    private static final SecretKey SECRET_KEY = resolveSecretKey();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static SecretKey resolveSecretKey() {
        String envKey = System.getenv("AISHIELD_VAULT_KEY");
        if (envKey == null || envKey.isBlank()) {
            envKey = System.getProperty("aishield.security.vault-key");
        }
        if (envKey != null && !envKey.isBlank()) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] derived = md.digest(envKey.getBytes(StandardCharsets.UTF_8));
                log.info("Initialized CardPanEncryptor with externalized vault key from environment/system property.");
                return new SecretKeySpec(derived, "AES");
            } catch (Exception e) {
                log.error("Failed to hash external vault key, falling back to default vault key: {}", e.getMessage());
            }
        }
        log.warn("SECURITY NOTICE: AISHIELD_VAULT_KEY not set. Using built-in development vault key. For production, set AISHIELD_VAULT_KEY in environment.");
        return new SecretKeySpec(DEFAULT_KEY_BYTES, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String rawAttribute) {
        if (rawAttribute == null || rawAttribute.isBlank()) {
            return rawAttribute;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, parameterSpec);

            byte[] cipherText = cipher.doFinal(rawAttribute.getBytes(StandardCharsets.UTF_8));

            // Combine IV + CipherText
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return "ENC:" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Error encrypting sensitive card data: {}", e.getMessage());
            return rawAttribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || !dbData.startsWith("ENC:")) {
            return dbData; // Return as-is if unencrypted legacy data
        }
        try {
            String base64Payload = dbData.substring(4);
            byte[] combined = Base64.getDecoder().decode(base64Payload);

            if (combined.length < IV_LENGTH_BYTE) {
                return dbData;
            }

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTE];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, parameterSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting sensitive card data: {}", e.getMessage());
            return dbData;
        }
    }
}