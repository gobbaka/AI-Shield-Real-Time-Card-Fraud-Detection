package com.aishield.fraud;

import com.aishield.fraud.config.CardPanEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardPanEncryptorTest {

    private CardPanEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new CardPanEncryptor();
    }

    @Test
    @DisplayName("Encryption should produce ciphertext with ENC: prefix and non-plaintext data")
    void testEncryptionFormat() {
        String plainCard = "4532-7590-1234-5678";
        String encrypted = encryptor.convertToDatabaseColumn(plainCard);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC:"), "Encrypted data must be prefixed with ENC:");
        assertFalse(encrypted.contains("4532"), "Encrypted payload must NOT contain plaintext card fragments");
        assertFalse(encrypted.contains("5678"), "Encrypted payload must NOT contain plaintext card fragments");
    }

    @Test
    @DisplayName("Round-trip encryption and decryption should recover exact original plaintext")
    void testRoundTripEncryptionDecryption() {
        String originalCard = "**** **** **** 4721";
        String encrypted = encryptor.convertToDatabaseColumn(originalCard);
        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        assertEquals(originalCard, decrypted, "Decrypted text must match the original plaintext exactly");
    }

    @Test
    @DisplayName("Each encryption should use a fresh random IV, producing distinct ciphertexts for identical plaintext")
    void testGcmUniqueInitializationVector() {
        String plain = "4111-2222-3333-4444";
        String cipher1 = encryptor.convertToDatabaseColumn(plain);
        String cipher2 = encryptor.convertToDatabaseColumn(plain);

        assertNotEquals(cipher1, cipher2, "Two encryptions of the same plaintext must produce different ciphertexts due to GCM random IV");
        assertEquals(plain, encryptor.convertToEntityAttribute(cipher1));
        assertEquals(plain, encryptor.convertToEntityAttribute(cipher2));
    }

    @Test
    @DisplayName("Null or blank strings should be preserved cleanly")
    void testNullOrBlankHandling() {
        assertNull(encryptor.convertToDatabaseColumn(null));
        assertEquals("", encryptor.convertToDatabaseColumn(""));
        assertEquals("   ", encryptor.convertToDatabaseColumn("   "));

        assertNull(encryptor.convertToEntityAttribute(null));
        assertEquals("", encryptor.convertToEntityAttribute(""));
    }

    @Test
    @DisplayName("Legacy unencrypted data without ENC: prefix should be returned as-is")
    void testLegacyUnencryptedData() {
        String legacy = "**** **** **** 9931";
        String result = encryptor.convertToEntityAttribute(legacy);
        assertEquals(legacy, result);
    }
}
