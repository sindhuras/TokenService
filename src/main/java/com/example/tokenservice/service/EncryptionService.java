package com.example.tokenservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionService {
    
    @Value("${encryption.key:myDefaultEncryptionKey123}")
    private String encryptionKey;
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 16;
    
    public String encrypt(String plainText) {
        try {
            SecretKey secretKey = generateSecretKey(encryptionKey);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    public String decrypt(String encryptedText) {
        try {
            SecretKey secretKey = generateSecretKey(encryptionKey);
            
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            
            byte[] iv = new byte[IV_SIZE];
            byte[] cipherText = new byte[combined.length - IV_SIZE];
            
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            System.arraycopy(combined, IV_SIZE, cipherText, 0, cipherText.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    private SecretKey generateSecretKey(String keyString) {
        try {
            byte[] key = keyString.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = new byte[KEY_SIZE / 8];
            
            if (key.length >= keyBytes.length) {
                System.arraycopy(key, 0, keyBytes, 0, keyBytes.length);
            } else {
                System.arraycopy(key, 0, keyBytes, 0, key.length);
                for (int i = key.length; i < keyBytes.length; i++) {
                    keyBytes[i] = 0;
                }
            }
            
            return new SecretKeySpec(keyBytes, ALGORITHM);
            
        } catch (Exception e) {
            log.error("Key generation failed", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }
}
