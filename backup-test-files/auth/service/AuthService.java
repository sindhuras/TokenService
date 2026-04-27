package com.example.tokenservice.auth.service;

import com.example.tokenservice.auth.entity.ApiToken;
import com.example.tokenservice.auth.entity.User;
import com.example.tokenservice.auth.repository.ApiTokenRepository;
import com.example.tokenservice.auth.repository.UserRepository;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.common.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final ApiTokenRepository apiTokenRepository;
    private final JwtService jwtService;
    
    @Transactional
    public String generateApiToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PaymentException("USER_NOT_FOUND", "User not found"));
        
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new PaymentException("USER_INACTIVE", "User account is not active");
        }
        
        String tokenId = EncryptionUtil.generateToken();
        String tokenValue = "Bearer " + EncryptionUtil.generateToken();
        String tokenHash = EncryptionUtil.encrypt(tokenValue);
        
        LocalDateTime expiryTime = LocalDateTime.now().plusDays(30);
        
        ApiToken apiToken = ApiToken.builder()
                .tokenId(tokenId)
                .userId(userId)
                .tokenHash(tokenHash)
                .expiryTime(expiryTime)
                .status("ACTIVE")
                .build();
        
        apiTokenRepository.save(apiToken);
        
        return tokenValue;
    }
    
    @Transactional
    public UUID validateToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new PaymentException("INVALID_TOKEN", "Invalid token format");
        }
        
        String tokenHash = EncryptionUtil.encrypt(token);
        ApiToken apiToken = apiTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new PaymentException("TOKEN_NOT_FOUND", "Token not found"));
        
        if (!"ACTIVE".equals(apiToken.getStatus())) {
            throw new PaymentException("TOKEN_INACTIVE", "Token is not active");
        }
        
        if (apiToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            apiToken.setStatus("EXPIRED");
            apiTokenRepository.save(apiToken);
            throw new PaymentException("TOKEN_EXPIRED", "Token has expired");
        }
        
        apiToken.setLastUsedAt(LocalDateTime.now());
        apiTokenRepository.save(apiToken);
        
        return apiToken.getUserId();
    }
    
    @Transactional
    public void revokeToken(String tokenId) {
        ApiToken apiToken = apiTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new PaymentException("TOKEN_NOT_FOUND", "Token not found"));
        
        apiToken.setStatus("REVOKED");
        apiTokenRepository.save(apiToken);
    }
    
    @Transactional
    public List<ApiToken> getActiveTokens(UUID userId) {
        return apiTokenRepository.findActiveTokensByUserId(userId, LocalDateTime.now());
    }
    
    @Transactional
    public User createUser(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new PaymentException("USER_EXISTS", "User with this email already exists");
        }
        
        User user = User.builder()
                .userId(UUID.randomUUID())
                .email(email)
                .passwordHash(EncryptionUtil.encrypt(password))
                .status("ACTIVE")
                .build();
        
        return userRepository.save(user);
    }
}
