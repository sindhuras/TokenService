package com.example.tokenservice.unit;

import com.example.tokenservice.auth.entity.ApiToken;
import com.example.tokenservice.auth.entity.User;
import com.example.tokenservice.auth.repository.ApiTokenRepository;
import com.example.tokenservice.auth.repository.UserRepository;
import com.example.tokenservice.auth.service.AuthService;
import com.example.tokenservice.auth.service.JwtService;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.common.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
public class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ApiTokenRepository apiTokenRepository;
    
    @Mock
    private JwtService jwtService;
    
    @InjectMocks
    private AuthService authService;
    
    private UUID userId;
    private User user;
    private ApiToken apiToken;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        user = User.builder()
                .userId(userId)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        
        apiToken = ApiToken.builder()
                .id(1L)
                .tokenId("tok_123")
                .userId(userId)
                .tokenHash("hashedToken")
                .expiryTime(LocalDateTime.now().plusDays(30))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGenerateApiTokenSuccess() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(apiTokenRepository.save(any(ApiToken.class))).thenReturn(apiToken);
        
        String token = authService.generateApiToken(userId);
        
        assertNotNull(token);
        assertTrue(token.startsWith("Bearer "));
        verify(userRepository).findById(userId);
        verify(apiTokenRepository).save(any(ApiToken.class));
    }
    
    @Test
    void testGenerateApiTokenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> authService.generateApiToken(userId));
        verify(userRepository).findById(userId);
        verify(apiTokenRepository, never()).save(any());
    }
    
    @Test
    void testGenerateApiTokenUserInactive() {
        user.setStatus("INACTIVE");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        assertThrows(PaymentException.class, () -> authService.generateApiToken(userId));
        verify(userRepository).findById(userId);
        verify(apiTokenRepository, never()).save(any());
    }
    
    @Test
    void testValidateTokenSuccess() {
        String token = "Bearer validToken";
        String tokenHash = "hashedToken";
        
        when(apiTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(apiToken));
        when(apiTokenRepository.save(any(ApiToken.class))).thenReturn(apiToken);
        
        UUID result = authService.validateToken(token);
        
        assertEquals(userId, result);
        verify(apiTokenRepository).findByTokenHash(tokenHash);
        verify(apiTokenRepository).save(apiToken);
    }
    
    @Test
    void testValidateTokenNullToken() {
        assertThrows(PaymentException.class, () -> authService.validateToken(null));
    }
    
    @Test
    void testValidateTokenInvalidFormat() {
        assertThrows(PaymentException.class, () -> authService.validateToken("InvalidToken"));
    }
    
    @Test
    void testValidateTokenNotFound() {
        String token = "Bearer invalidToken";
        String tokenHash = "hashedInvalidToken";
        
        when(apiTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> authService.validateToken(token));
        verify(apiTokenRepository).findByTokenHash(tokenHash);
    }
    
    @Test
    void testValidateTokenInactive() {
        String token = "Bearer inactiveToken";
        String tokenHash = "hashedInactiveToken";
        apiToken.setStatus("INACTIVE");
        
        when(apiTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(apiToken));
        
        assertThrows(PaymentException.class, () -> authService.validateToken(token));
        verify(apiTokenRepository).findByTokenHash(tokenHash);
    }
    
    @Test
    void testValidateTokenExpired() {
        String token = "Bearer expiredToken";
        String tokenHash = "hashedExpiredToken";
        apiToken.setExpiryTime(LocalDateTime.now().minusDays(1));
        
        when(apiTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(apiToken));
        when(apiTokenRepository.save(any(ApiToken.class))).thenReturn(apiToken);
        
        assertThrows(PaymentException.class, () -> authService.validateToken(token));
        verify(apiTokenRepository).findByTokenHash(tokenHash);
        verify(apiTokenRepository).save(apiToken);
    }
    
    @Test
    void testRevokeTokenSuccess() {
        String tokenId = "tok_123";
        
        when(apiTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(apiToken));
        when(apiTokenRepository.save(any(ApiToken.class))).thenReturn(apiToken);
        
        authService.revokeToken(tokenId);
        
        assertEquals("REVOKED", apiToken.getStatus());
        verify(apiTokenRepository).findByTokenId(tokenId);
        verify(apiTokenRepository).save(apiToken);
    }
    
    @Test
    void testRevokeTokenNotFound() {
        String tokenId = "nonexistent";
        
        when(apiTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> authService.revokeToken(tokenId));
        verify(apiTokenRepository).findByTokenId(tokenId);
        verify(apiTokenRepository, never()).save(any());
    }
    
    @Test
    void testGetActiveTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<ApiToken> expectedTokens = List.of(apiToken);
        
        when(apiTokenRepository.findActiveTokensByUserId(userId, now)).thenReturn(expectedTokens);
        
        List<ApiToken> result = authService.getActiveTokens(userId);
        
        assertEquals(expectedTokens, result);
        verify(apiTokenRepository).findActiveTokensByUserId(userId, now);
    }
    
    @Test
    void testCreateUserSuccess() {
        String email = "newuser@example.com";
        String password = "password123";
        
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = authService.createUser(email, password);
        
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(userRepository).existsByEmail(email);
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testCreateUserEmailExists() {
        String email = "existing@example.com";
        String password = "password123";
        
        when(userRepository.existsByEmail(email)).thenReturn(true);
        
        assertThrows(PaymentException.class, () -> authService.createUser(email, password));
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any());
    }
}
