package com.example.tokenservice.unit;

import com.example.tokenservice.model.PaymentToken;
import com.example.tokenservice.payment.repository.PaymentTokenRepository;
import com.example.tokenservice.payment.service.PaymentTokenService;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.common.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentTokenServiceTest {
    
    @Mock
    private PaymentTokenRepository paymentTokenRepository;
    
    @InjectMocks
    private PaymentTokenService paymentTokenService;
    
    private UUID userId;
    private PaymentToken paymentToken;
    private String tokenId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tokenId = "tok_123456";
        
        paymentToken = PaymentToken.builder()
                .id(1L)
                .tokenId(tokenId)
                .userId(userId)
                .tokenValue("tok_value_123")
                .encryptedData("encryptedData")
                .maskedData("****-****-****-1234")
                .dataType("CARD")
                .expiryTime(LocalDateTime.now().plusYears(3))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testCreateTokenCardType() {
        String paymentData = "4111111111111111";
        String dataType = "CARD";
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            mockedEncryption.when(() -> EncryptionUtil.encrypt(anyString())).thenReturn("encrypted");
            mockedEncryption.when(() -> EncryptionUtil.maskCardNumber(anyString())).thenReturn("****-****-****-1234");
            
            when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
            
            PaymentToken result = paymentTokenService.createToken(userId, paymentData, dataType);
            
            assertNotNull(result);
            assertEquals(userId, result.getUserId());
            assertEquals("CARD", result.getDataType());
            assertEquals("ACTIVE", result.getStatus());
            assertNotNull(result.getExpiryTime());
            verify(paymentTokenRepository).save(any(PaymentToken.class));
        }
    }
    
    @Test
    void testCreateTokenUpiType() {
        String paymentData = "user@upi";
        String dataType = "UPI";
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            mockedEncryption.when(() -> EncryptionUtil.encrypt(anyString())).thenReturn("encrypted");
            
            when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
            
            PaymentToken result = paymentTokenService.createToken(userId, paymentData, dataType);
            
            assertNotNull(result);
            assertEquals("UPI", result.getDataType());
            assertNull(result.getExpiryTime()); // UPI tokens don't expire
            verify(paymentTokenRepository).save(any(PaymentToken.class));
        }
    }
    
    @Test
    void testCreateTokenBankType() {
        String paymentData = "1234567890123456";
        String dataType = "BANK";
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            mockedEncryption.when(() -> EncryptionUtil.encrypt(anyString())).thenReturn("encrypted");
            
            when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
            
            PaymentToken result = paymentTokenService.createToken(userId, paymentData, dataType);
            
            assertNotNull(result);
            assertEquals("BANK", result.getDataType());
            assertNull(result.getExpiryTime()); // Bank tokens don't expire
            verify(paymentTokenRepository).save(any(PaymentToken.class));
        }
    }
    
    @Test
    void testDecryptTokenSuccess() {
        String tokenId = "tok_123";
        String decryptedData = "4111111111111111";
        
        when(paymentTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(paymentToken));
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.decrypt(anyString())).thenReturn(decryptedData);
            when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
            
            String result = paymentTokenService.decryptToken(tokenId);
            
            assertEquals(decryptedData, result);
            verify(paymentTokenRepository).findByTokenId(tokenId);
            verify(paymentTokenRepository).save(paymentToken);
        }
    }
    
    @Test
    void testDecryptTokenNotFound() {
        String tokenId = "nonexistent";
        
        when(paymentTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> paymentTokenService.decryptToken(tokenId));
        verify(paymentTokenRepository).findByTokenId(tokenId);
        verify(paymentTokenRepository, never()).save(any());
    }
    
    @Test
    void testDecryptTokenInactive() {
        String tokenId = "tok_123";
        paymentToken.setStatus("INACTIVE");
        
        when(paymentTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(paymentToken));
        
        assertThrows(PaymentException.class, () -> paymentTokenService.decryptToken(tokenId));
        verify(paymentTokenRepository).findByTokenId(tokenId);
        verify(paymentTokenRepository, never()).save(any());
    }
    
    @Test
    void testDecryptTokenExpired() {
        String tokenId = "tok_123";
        paymentToken.setExpiryTime(LocalDateTime.now().minusDays(1));
        
        when(paymentTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(paymentToken));
        when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
        
        assertThrows(PaymentException.class, () -> paymentTokenService.decryptToken(tokenId));
        verify(paymentTokenRepository).findByTokenId(tokenId);
        verify(paymentTokenRepository).save(paymentToken);
        assertEquals("EXPIRED", paymentToken.getStatus());
    }
    
    @Test
    void testRevokeTokenSuccess() {
        String tokenId = "tok_123";
        
        when(paymentTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(paymentToken));
        when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
        
        paymentTokenService.revokeToken(tokenId);
        
        assertEquals("REVOKED", paymentToken.getStatus());
        verify(paymentTokenRepository).findByTokenId(tokenId);
        verify(paymentTokenRepository).save(paymentToken);
    }
    
    @Test
    void testRevokeTokenNotFound() {
        String tokenId = "nonexistent";
        
        when(paymentTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> paymentTokenService.revokeToken(tokenId));
        verify(paymentTokenRepository).findByTokenId(tokenId);
        verify(paymentTokenRepository, never()).save(any());
    }
    
    @Test
    void testGetActiveTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentToken> expectedTokens = List.of(paymentToken);
        
        when(paymentTokenRepository.findActiveTokensByUserId(userId, now)).thenReturn(expectedTokens);
        
        List<PaymentToken> result = paymentTokenService.getActiveTokens(userId);
        
        assertEquals(expectedTokens, result);
        verify(paymentTokenRepository).findActiveTokensByUserId(userId, now);
    }
    
    @Test
    void testGetTokenSuccess() {
        String tokenId = "tok_123";
        
        when(paymentTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(paymentToken));
        
        PaymentToken result = paymentTokenService.getToken(tokenId);
        
        assertEquals(paymentToken, result);
        verify(paymentTokenRepository).findByTokenId(tokenId);
    }
    
    @Test
    void testGetTokenNotFound() {
        String tokenId = "nonexistent";
        
        when(paymentTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> paymentTokenService.getToken(tokenId));
        verify(paymentTokenRepository).findByTokenId(tokenId);
    }
    
    @Test
    void testMaskPaymentDataCard() {
        String paymentData = "4111111111111111";
        String dataType = "CARD";
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.maskCardNumber(anyString())).thenReturn("****-****-****-1234");
            
            when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
            
            PaymentToken result = paymentTokenService.createToken(userId, paymentData, dataType);
            
            assertNotNull(result);
            verify(paymentTokenRepository).save(any(PaymentToken.class));
        }
    }
    
    @Test
    void testMaskPaymentDataUpi() {
        String paymentData = "user@upi";
        String dataType = "UPI";
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            mockedEncryption.when(() -> EncryptionUtil.encrypt(anyString())).thenReturn("encrypted");
            
            when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
            
            PaymentToken result = paymentTokenService.createToken(userId, paymentData, dataType);
            
            assertNotNull(result);
            assertTrue(result.getMaskedData().contains("***"));
            verify(paymentTokenRepository).save(any(PaymentToken.class));
        }
    }
    
    @Test
    void testMaskPaymentDataBank() {
        String paymentData = "1234567890123456";
        String dataType = "BANK";
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            mockedEncryption.when(() -> EncryptionUtil.encrypt(anyString())).thenReturn("encrypted");
            
            when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
            
            PaymentToken result = paymentTokenService.createToken(userId, paymentData, dataType);
            
            assertNotNull(result);
            assertTrue(result.getMaskedData().contains("******"));
            verify(paymentTokenRepository).save(any(PaymentToken.class));
        }
    }
    
    @Test
    void testMaskPaymentDataDefault() {
        String paymentData = "unknown";
        String dataType = "UNKNOWN";
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            mockedEncryption.when(() -> EncryptionUtil.encrypt(anyString())).thenReturn("encrypted");
            
            when(paymentTokenRepository.save(any(PaymentToken.class))).thenReturn(paymentToken);
            
            PaymentToken result = paymentTokenService.createToken(userId, paymentData, dataType);
            
            assertNotNull(result);
            assertEquals("****", result.getMaskedData());
            verify(paymentTokenRepository).save(any(PaymentToken.class));
        }
    }
}
