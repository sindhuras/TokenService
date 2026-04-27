package com.example.tokenservice.unit;

import com.example.tokenservice.common.util.EncryptionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EncryptionUtilTest {
    
    @Test
    void testEncryptDecrypt() {
        String originalData = "sensitive_card_data_123456789";
        
        String encryptedData = EncryptionUtil.encrypt(originalData);
        String decryptedData = EncryptionUtil.decrypt(encryptedData);
        
        assertNotNull(encryptedData);
        assertNotNull(decryptedData);
        assertNotEquals(originalData, encryptedData);
        assertEquals(originalData, decryptedData);
    }
    
    @Test
    void testEncryptConsistency() {
        String originalData = "test_data";
        
        String encrypted1 = EncryptionUtil.encrypt(originalData);
        String encrypted2 = EncryptionUtil.encrypt(originalData);
        
        assertNotNull(encrypted1);
        assertNotNull(encrypted2);
        assertEquals(encrypted1, encrypted2); // Should be consistent for same input
    }
    
    @Test
    void testDecryptInvalidData() {
        String invalidEncryptedData = "invalid_encrypted_data";
        
        assertThrows(RuntimeException.class, () -> EncryptionUtil.decrypt(invalidEncryptedData));
    }
    
    @Test
    void testEncryptNullData() {
        assertThrows(RuntimeException.class, () -> EncryptionUtil.encrypt(null));
    }
    
    @Test
    void testDecryptNullData() {
        assertThrows(RuntimeException.class, () -> EncryptionUtil.decrypt(null));
    }
    
    @Test
    void testMaskCardNumber16Digits() {
        String cardNumber = "4111111111111111";
        String masked = EncryptionUtil.maskCardNumber(cardNumber);
        
        assertEquals("****-****-****-1111", masked);
    }
    
    @Test
    void testMaskCardNumberWithSpaces() {
        String cardNumber = "4111 1111 1111 1111";
        String masked = EncryptionUtil.maskCardNumber(cardNumber);
        
        assertEquals("****-****-****-1111", masked);
    }
    
    @Test
    void testMaskCardNumberWithHyphens() {
        String cardNumber = "4111-1111-1111-1111";
        String masked = EncryptionUtil.maskCardNumber(cardNumber);
        
        assertEquals("****-****-****-1111", masked);
    }
    
    @Test
    void testMaskCardNumber15Digits() {
        String cardNumber = "378282246310005";
        String masked = EncryptionUtil.maskCardNumber(cardNumber);
        
        assertEquals("****-****-****-0005", masked);
    }
    
    @Test
    void testMaskCardNumber13Digits() {
        String cardNumber = "6011111111117";
        String masked = EncryptionUtil.maskCardNumber(cardNumber);
        
        assertEquals("****-****-****-1117", masked);
    }
    
    @Test
    void testMaskCardNumberNull() {
        String masked = EncryptionUtil.maskCardNumber(null);
        
        assertEquals("****", masked);
    }
    
    @Test
    void testMaskCardNumberEmpty() {
        String masked = EncryptionUtil.maskCardNumber("");
        
        assertEquals("****", masked);
    }
    
    @Test
    void testMaskCardNumberTooShort() {
        String cardNumber = "123";
        String masked = EncryptionUtil.maskCardNumber(cardNumber);
        
        assertEquals("****", masked);
    }
    
    @Test
    void testMaskCardNumberExactly4Digits() {
        String cardNumber = "1234";
        String masked = EncryptionUtil.maskCardNumber(cardNumber);
        
        assertEquals("****-****-****-1234", masked);
    }
    
    @Test
    void testGenerateToken() {
        String token1 = EncryptionUtil.generateToken();
        String token2 = EncryptionUtil.generateToken();
        
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertFalse(token1.isEmpty());
        assertFalse(token2.isEmpty());
    }
    
    @Test
    void testGenerateTokenUniqueness() {
        java.util.Set<String> tokens = new java.util.HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            String token = EncryptionUtil.generateToken();
            assertTrue(tokens.add(token), "Generated token should be unique: " + token);
        }
    }
    
    @Test
    void testEncryptEmptyString() {
        String originalData = "";
        
        String encryptedData = EncryptionUtil.encrypt(originalData);
        String decryptedData = EncryptionUtil.decrypt(encryptedData);
        
        assertNotNull(encryptedData);
        assertNotNull(decryptedData);
        assertEquals(originalData, decryptedData);
    }
    
    @Test
    void testEncryptSpecialCharacters() {
        String originalData = "card@#$%^&*()_+-={}[]|\\:;\"'<>?,./";
        
        String encryptedData = EncryptionUtil.encrypt(originalData);
        String decryptedData = EncryptionUtil.decrypt(encryptedData);
        
        assertNotNull(encryptedData);
        assertNotNull(decryptedData);
        assertEquals(originalData, decryptedData);
    }
    
    @Test
    void testEncryptLongData() {
        StringBuilder longData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longData.append("a");
        }
        String originalData = longData.toString();
        
        String encryptedData = EncryptionUtil.encrypt(originalData);
        String decryptedData = EncryptionUtil.decrypt(encryptedData);
        
        assertNotNull(encryptedData);
        assertNotNull(decryptedData);
        assertEquals(originalData, decryptedData);
    }
    
    @Test
    void testMaskCardNumberWithMixedSeparators() {
        String cardNumber = "4111-1111 1111-1111";
        String masked = EncryptionUtil.maskCardNumber(cardNumber);
        
        assertEquals("****-****-****-1111", masked);
    }
    
    @Test
    void testMaskCardNumberWithNonNumeric() {
        String cardNumber = "abcd-efgh-ijkl-mnop";
        String masked = EncryptionUtil.maskCardNumber(cardNumber);
        
        assertEquals("****-****-****-mnop", masked);
    }
}
