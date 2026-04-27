package com.example.tokenservice.service;

import com.example.tokenservice.dto.CreateTokenRequest;
import com.example.tokenservice.dto.TokenResponse;
import com.example.tokenservice.entity.TokenVault;
import com.example.tokenservice.exception.TokenServiceException;
import com.example.tokenservice.repository.TokenVaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final TokenVaultRepository tokenVaultRepository;
    private final EncryptionService encryptionService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${token.cache.ttl:3600}")
    private long cacheTtl;
    
    @Value("${token.max-active-per-user:10}")
    private int maxActiveTokensPerUser;
    
    @Transactional
    public TokenResponse createToken(CreateTokenRequest request) {
        log.info("Creating token for user: {}", request.getUserId());
        
        // Validate user token limit
        long activeTokenCount = tokenVaultRepository.countActiveTokensByUser(request.getUserId());
        if (activeTokenCount >= maxActiveTokensPerUser) {
            throw new TokenServiceException("Maximum active tokens limit reached for user");
        }
        
        // Generate secure token ID
        String tokenId = generateSecureTokenId();
        
        // Encrypt sensitive data
        String encryptedData = encryptionService.encrypt(request.getPaymentData());
        String maskedData = maskSensitiveData(request.getPaymentData(), request.getDataType());
        
        // Create token entity
        TokenVault token = TokenVault.builder()
                .tokenId(tokenId)
                .userId(request.getUserId())
                .tokenValue(tokenId) // Store token ID as value for simplicity
                .encryptedData(encryptedData)
                .maskedData(maskedData)
                .dataType(request.getDataType())
                .expiryTime(request.getExpiryTime())
                .maxUsageLimit(request.getMaxUsageLimit())
                .status("ACTIVE")
                .build();
        
        token = tokenVaultRepository.save(token);
        
        // Cache the token
        cacheToken(token);
        
        log.info("Successfully created token: {} for user: {}", tokenId, request.getUserId());
        return mapToTokenResponse(token);
    }
    
    @Transactional(readOnly = true)
    public TokenResponse getToken(String tokenId) {
        log.debug("Retrieving token: {}", tokenId);
        
        // Check cache first
        TokenVault cachedToken = getCachedToken(tokenId);
        if (cachedToken != null) {
            log.debug("Token {} found in cache", tokenId);
            return mapToTokenResponse(cachedToken);
        }
        
        // Retrieve from database
        TokenVault token = tokenVaultRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new TokenServiceException("Token not found: " + tokenId));
        
        // Update usage count
        token.setUsageCount(token.getUsageCount() + 1);
        token.setLastUsedAt(LocalDateTime.now());
        
        // Check usage limit
        if (token.getMaxUsageLimit() != null && token.getUsageCount() >= token.getMaxUsageLimit()) {
            token.setStatus("EXPIRED");
        }
        
        tokenVaultRepository.save(token);
        cacheToken(token);
        
        return mapToTokenResponse(token);
    }
    
    @Transactional(readOnly = true)
    public List<TokenResponse> getUserTokens(UUID userId) {
        log.debug("Retrieving tokens for user: {}", userId);
        
        List<TokenVault> tokens = tokenVaultRepository.findByUserIdAndStatus(userId, "ACTIVE");
        return tokens.stream()
                .map(this::mapToTokenResponse)
                .toList();
    }
    
    @Transactional
    public void revokeToken(String tokenId) {
        log.info("Revoking token: {}", tokenId);
        
        TokenVault token = tokenVaultRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new TokenServiceException("Token not found: " + tokenId));
        
        token.setStatus("REVOKED");
        tokenVaultRepository.save(token);
        
        // Remove from cache
        redisTemplate.delete("token:" + tokenId);
        
        log.info("Successfully revoked token: {}", tokenId);
    }
    
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired tokens");
        
        List<TokenVault> expiredTokens = tokenVaultRepository.findExpiredTokens(LocalDateTime.now());
        expiredTokens.forEach(token -> {
            token.setStatus("EXPIRED");
            redisTemplate.delete("token:" + token.getTokenId());
        });
        
        List<TokenVault> usageLimitTokens = tokenVaultRepository.findTokensWithUsageLimitReached();
        usageLimitTokens.forEach(token -> {
            token.setStatus("EXPIRED");
            redisTemplate.delete("token:" + token.getTokenId());
        });
        
        tokenVaultRepository.saveAll(expiredTokens);
        tokenVaultRepository.saveAll(usageLimitTokens);
        
        log.info("Cleaned up {} expired tokens", expiredTokens.size() + usageLimitTokens.size());
    }
    
    private String generateSecureTokenId() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().toString();
    }
    
    private String maskSensitiveData(String data, String dataType) {
        if (data == null || data.length() < 8) {
            return "****";
        }
        
        return switch (dataType) {
            case "CARD" -> data.substring(0, 4) + "****" + data.substring(data.length() - 4);
            case "UPI" -> data.substring(0, 3) + "****@" + data.substring(data.lastIndexOf('@') + 1);
            case "BANK" -> "****" + data.substring(data.length() - 4);
            default -> "****";
        };
    }
    
    private void cacheToken(TokenVault token) {
        try {
            redisTemplate.opsForValue().set("token:" + token.getTokenId(), token, cacheTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache token: {}", token.getTokenId(), e);
        }
    }
    
    private TokenVault getCachedToken(String tokenId) {
        try {
            return (TokenVault) redisTemplate.opsForValue().get("token:" + tokenId);
        } catch (Exception e) {
            log.warn("Failed to retrieve token from cache: {}", tokenId, e);
            return null;
        }
    }
    
    private TokenResponse mapToTokenResponse(TokenVault token) {
        return TokenResponse.builder()
                .tokenId(token.getTokenId())
                .userId(token.getUserId())
                .maskedData(token.getMaskedData())
                .dataType(token.getDataType())
                .expiryTime(token.getExpiryTime())
                .status(token.getStatus())
                .createdAt(token.getCreatedAt())
                .usageCount(token.getUsageCount())
                .maxUsageLimit(token.getMaxUsageLimit())
                .build();
    }
}
