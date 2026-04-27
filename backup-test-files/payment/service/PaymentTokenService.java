package com.example.tokenservice.payment.service;

import com.example.tokenservice.model.PaymentToken;
import com.example.tokenservice.payment.repository.PaymentTokenRepository;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.common.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTokenService {
    
    private final PaymentTokenRepository paymentTokenRepository;
    
    @Transactional
    public PaymentToken createToken(UUID userId, String paymentData, String dataType) {
        String tokenId = EncryptionUtil.generateToken();
        String tokenValue = "tok_" + EncryptionUtil.generateToken();
        String encryptedData = EncryptionUtil.encrypt(paymentData);
        String maskedData = maskPaymentData(paymentData, dataType);
        
        LocalDateTime expiryTime = null;
        if ("CARD".equals(dataType)) {
            expiryTime = LocalDateTime.now().plusYears(3);
        }
        
        PaymentToken paymentToken = PaymentToken.builder()
                .tokenId(tokenId)
                .userId(userId)
                .tokenValue(tokenValue)
                .encryptedData(encryptedData)
                .maskedData(maskedData)
                .dataType(dataType)
                .expiryTime(expiryTime)
                .status("ACTIVE")
                .build();
        
        return paymentTokenRepository.save(paymentToken);
    }
    
    @Transactional
    public String decryptToken(String tokenId) {
        PaymentToken token = paymentTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new PaymentException("TOKEN_NOT_FOUND", "Payment token not found"));
        
        if (!"ACTIVE".equals(token.getStatus())) {
            throw new PaymentException("TOKEN_INACTIVE", "Payment token is not active");
        }
        
        if (token.getExpiryTime() != null && token.getExpiryTime().isBefore(LocalDateTime.now())) {
            token.setStatus("EXPIRED");
            paymentTokenRepository.save(token);
            throw new PaymentException("TOKEN_EXPIRED", "Payment token has expired");
        }
        
        token.setLastUsedAt(LocalDateTime.now());
        paymentTokenRepository.save(token);
        
        return EncryptionUtil.decrypt(token.getEncryptedData());
    }
    
    @Transactional
    public void revokeToken(String tokenId) {
        PaymentToken token = paymentTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new PaymentException("TOKEN_NOT_FOUND", "Payment token not found"));
        
        token.setStatus("REVOKED");
        paymentTokenRepository.save(token);
    }
    
    public List<PaymentToken> getActiveTokens(UUID userId) {
        return paymentTokenRepository.findActiveTokensByUserId(userId, LocalDateTime.now());
    }
    
    public PaymentToken getToken(String tokenId) {
        return paymentTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new PaymentException("TOKEN_NOT_FOUND", "Payment token not found"));
    }
    
    private String maskPaymentData(String paymentData, String dataType) {
        switch (dataType) {
            case "CARD":
                return EncryptionUtil.maskCardNumber(paymentData.replace("-", "").replace(" ", ""));
            case "UPI":
                if (paymentData.contains("@")) {
                    String[] parts = paymentData.split("@");
                    return parts[0].substring(0, 2) + "***@" + parts[1];
                }
                return paymentData.substring(0, 2) + "***";
            case "BANK":
                return paymentData.substring(0, 4) + "******" + paymentData.substring(paymentData.length() - 4);
            default:
                return "****";
        }
    }
}
