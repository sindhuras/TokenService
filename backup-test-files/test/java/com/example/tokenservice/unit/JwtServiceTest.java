package com.example.tokenservice.unit;

import com.example.tokenservice.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {
    
    @InjectMocks
    private JwtService jwtService;
    
    private String secret = "testSecretKeyForJwtTokenGeneration123456789";
    private Long expiration = 86400000L; // 24 hours
    private String email = "test@example.com";
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", expiration);
    }
    
    @Test
    void testGenerateToken() {
        String token = jwtService.generateToken(userId, email);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }
    
    @Test
    void testExtractUsername() {
        String token = jwtService.generateToken(userId, email);
        
        String extractedUsername = jwtService.extractUsername(token);
        
        assertEquals(email, extractedUsername);
    }
    
    @Test
    void testExtractExpiration() {
        String token = jwtService.generateToken(userId, email);
        
        Date expiration = jwtService.extractExpiration(token);
        
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }
    
    @Test
    void testExtractClaim() {
        String token = jwtService.generateToken(userId, email);
        
        Object userIdClaim = jwtService.extractClaim(token, claims -> claims.get("userId"));
        
        assertNotNull(userIdClaim);
        assertEquals(userId.toString(), userIdClaim);
    }
    
    @Test
    void testValidateTokenValid() {
        String token = jwtService.generateToken(userId, email);
        
        boolean isValid = jwtService.validateToken(token, email);
        
        assertTrue(isValid);
    }
    
    @Test
    void testValidateTokenInvalidEmail() {
        String token = jwtService.generateToken(userId, email);
        String wrongEmail = "wrong@example.com";
        
        boolean isValid = jwtService.validateToken(token, wrongEmail);
        
        assertFalse(isValid);
    }
    
    @Test
    void testValidateTokenExpired() {
        // Create token with very short expiration
        ReflectionTestUtils.setField(jwtService, "expiration", 1L); // 1 millisecond
        String token = jwtService.generateToken(userId, email);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        boolean isValid = jwtService.validateToken(token, email);
        
        assertFalse(isValid);
    }
    
    @Test
    void testValidateTokenInvalidFormat() {
        String invalidToken = "invalid.token.format";
        
        boolean isValid = jwtService.validateToken(invalidToken, email);
        
        assertFalse(isValid);
    }
    
    @Test
    void testValidateTokenNull() {
        boolean isValid = jwtService.validateToken(null, email);
        
        assertFalse(isValid);
    }
    
    @Test
    void testValidateTokenEmpty() {
        boolean isValid = jwtService.validateToken("", email);
        
        assertFalse(isValid);
    }
    
    @Test
    void testExtractUsernameNullToken() {
        assertThrows(Exception.class, () -> jwtService.extractUsername(null));
    }
    
    @Test
    void testExtractExpirationNullToken() {
        assertThrows(Exception.class, () -> jwtService.extractExpiration(null));
    }
    
    @Test
    void testExtractClaimNullToken() {
        assertThrows(Exception.class, () -> jwtService.extractClaim(null, claims -> claims.getSubject()));
    }
    
    @Test
    void testGenerateTokenDifferentUsers() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        
        String token1 = jwtService.generateToken(userId1, email1);
        String token2 = jwtService.generateToken(userId2, email2);
        
        assertNotEquals(token1, token2);
        assertEquals(email1, jwtService.extractUsername(token1));
        assertEquals(email2, jwtService.extractUsername(token2));
    }
    
    @Test
    void testTokenExpirationTime() {
        long beforeCreation = System.currentTimeMillis();
        String token = jwtService.generateToken(userId, email);
        long afterCreation = System.currentTimeMillis();
        
        Date expiration = jwtService.extractExpiration(token);
        long expirationTime = expiration.getTime();
        
        // Expiration should be approximately creation time + expiration period
        long expectedMin = beforeCreation + expiration;
        long expectedMax = afterCreation + expiration;
        
        assertTrue(expirationTime >= expectedMin && expirationTime <= expectedMax,
                "Token expiration time should be within expected range");
    }
    
    @Test
    void testIsTokenExpired() throws Exception {
        // Create token with very short expiration
        ReflectionTestUtils.setField(jwtService, "expiration", 1L); // 1 millisecond
        String token = jwtService.generateToken(userId, email);
        
        // Wait for token to expire
        Thread.sleep(10);
        
        // Use reflection to test private method
        java.lang.reflect.Method isTokenExpired = JwtService.class.getDeclaredMethod("isTokenExpired", String.class);
        isTokenExpired.setAccessible(true);
        boolean expired = (Boolean) isTokenExpired.invoke(jwtService, token);
        
        assertTrue(expired);
    }
    
    @Test
    void testCreateToken() throws Exception {
        // Use reflection to test private method
        java.lang.reflect.Method createToken = JwtService.class.getDeclaredMethod(
                "createToken", java.util.Map.class, String.class);
        createToken.setAccessible(true);
        
        java.util.Map<String, Object> claims = java.util.Map.of("customClaim", "customValue");
        String subject = "testSubject";
        
        String token = (String) createToken.invoke(jwtService, claims, subject);
        
        assertNotNull(token);
        assertEquals(subject, jwtService.extractUsername(token));
    }
}
