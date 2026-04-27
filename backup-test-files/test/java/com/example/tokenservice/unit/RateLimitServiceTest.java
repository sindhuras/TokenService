package com.example.tokenservice.unit;

import com.example.tokenservice.cache.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RateLimitServiceTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @InjectMocks
    private RateLimitService rateLimitService;
    
    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(redisTemplate);
    }
    
    @Test
    void testCheckRateLimitAllowedFirstRequest() {
        String key = "test_key";
        int limit = 10;
        int windowSeconds = 60;
        
        Object[] scriptResult = {1, 9}; // current count, remaining
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(scriptResult);
        
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(key, limit, windowSeconds);
        
        assertTrue(result.isAllowed());
        assertEquals(1, result.getCurrentCount());
        assertEquals(9, result.getRemaining());
        assertEquals(windowSeconds, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
    }
    
    @Test
    void testCheckRateLimitAllowedSubsequentRequest() {
        String key = "test_key";
        int limit = 10;
        int windowSeconds = 60;
        
        Object[] scriptResult = {5, 5}; // current count, remaining
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(scriptResult);
        
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(key, limit, windowSeconds);
        
        assertTrue(result.isAllowed());
        assertEquals(5, result.getCurrentCount());
        assertEquals(5, result.getRemaining());
        assertEquals(windowSeconds, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
    }
    
    @Test
    void testCheckRateLimitBlocked() {
        String key = "test_key";
        int limit = 10;
        int windowSeconds = 60;
        
        Object[] scriptResult = {10, 0}; // current count, remaining
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(scriptResult);
        
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(key, limit, windowSeconds);
        
        assertFalse(result.isAllowed());
        assertEquals(10, result.getCurrentCount());
        assertEquals(0, result.getRemaining());
        assertEquals(windowSeconds, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
    }
    
    @Test
    void testCheckRateLimitScriptExecutionFails() {
        String key = "test_key";
        int limit = 10;
        int windowSeconds = 60;
        
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(null);
        
        when(redisTemplate.opsForValue()).get(key)).thenReturn(null);
        when(redisTemplate.opsForValue()).set(key, "1", windowSeconds, java.util.concurrent.TimeUnit.SECONDS))
                .thenReturn(true);
        
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(key, limit, windowSeconds);
        
        assertTrue(result.isAllowed()); // Should allow on fallback
        assertEquals(1, result.getCurrentCount());
        assertEquals(9, result.getRemaining());
        assertEquals(windowSeconds, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
        verify(redisTemplate.opsForValue()).get(key);
        verify(redisTemplate.opsForValue()).set(key, "1", windowSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    @Test
    void testCheckRateLimitFallbackWithExistingKey() {
        String key = "test_key";
        int limit = 10;
        int windowSeconds = 60;
        
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(null);
        when(redisTemplate.opsForValue()).get(key)).thenReturn("5");
        when(redisTemplate.opsForValue().increment(key)).thenReturn(6L);
        
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(key, limit, windowSeconds);
        
        assertTrue(result.isAllowed());
        assertEquals(6, result.getCurrentCount());
        assertEquals(4, result.getRemaining());
        assertEquals(windowSeconds, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
        verify(redisTemplate.opsForValue()).get(key);
        verify(redisTemplate.opsForValue()).increment(key);
    }
    
    @Test
    void testCheckRateLimitFallbackLimitExceeded() {
        String key = "test_key";
        int limit = 10;
        int windowSeconds = 60;
        
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(null);
        when(redisTemplate.opsForValue()).get(key)).thenReturn("10");
        
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(key, limit, windowSeconds);
        
        assertFalse(result.isAllowed());
        assertEquals(10, result.getCurrentCount());
        assertEquals(0, result.getRemaining());
        assertEquals(windowSeconds, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
        verify(redisTemplate.opsForValue()).get(key);
    }
    
    @Test
    void testResetRateLimit() {
        String key = "test_key";
        
        when(redisTemplate.delete(key)).thenReturn(true);
        
        rateLimitService.resetRateLimit(key);
        
        verify(redisTemplate).delete(key);
    }
    
    @Test
    void testCheckApiRateLimit() {
        String userId = "user123";
        String endpoint = "/api/payments";
        
        Object[] scriptResult = {1, 99};
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(scriptResult);
        
        RateLimitService.RateLimitResult result = rateLimitService.checkApiRateLimit(userId, endpoint);
        
        assertTrue(result.isAllowed());
        assertEquals(1, result.getCurrentCount());
        assertEquals(99, result.getRemaining());
        assertEquals(3600, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
    }
    
    @Test
    void testCheckPaymentRateLimit() {
        String userId = "user123";
        
        Object[] scriptResult = {1, 9};
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(scriptResult);
        
        RateLimitService.RateLimitResult result = rateLimitService.checkPaymentRateLimit(userId);
        
        assertTrue(result.isAllowed());
        assertEquals(1, result.getCurrentCount());
        assertEquals(9, result.getRemaining());
        assertEquals(60, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
    }
    
    @Test
    void testCheckAuthRateLimit() {
        String identifier = "user@example.com";
        
        Object[] scriptResult = {1, 4};
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(scriptResult);
        
        RateLimitService.RateLimitResult result = rateLimitService.checkAuthRateLimit(identifier);
        
        assertTrue(result.isAllowed());
        assertEquals(1, result.getCurrentCount());
        assertEquals(4, result.getRemaining());
        assertEquals(300, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
    }
    
    @Test
    void testCheckRateLimitWithEmptyResult() {
        String key = "test_key";
        int limit = 10;
        int windowSeconds = 60;
        
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenReturn(new Object[0]);
        
        when(redisTemplate.opsForValue()).get(key)).thenReturn(null);
        when(redisTemplate.opsForValue()).set(key, "1", windowSeconds, java.util.concurrent.TimeUnit.SECONDS))
                .thenReturn(true);
        
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(key, limit, windowSeconds);
        
        assertTrue(result.isAllowed());
        assertEquals(1, result.getCurrentCount());
        assertEquals(9, result.getRemaining());
        assertEquals(windowSeconds, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
        verify(redisTemplate.opsForValue()).get(key);
        verify(redisTemplate.opsForValue()).set(key, "1", windowSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    @Test
    void testCheckRateLimitWithException() {
        String key = "test_key";
        int limit = 10;
        int windowSeconds = 60;
        
        when(redisTemplate.execute(any(RedisScript.class), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));
        
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(key, limit, windowSeconds);
        
        assertTrue(result.isAllowed()); // Should allow on exception
        assertEquals(0, result.getCurrentCount());
        assertEquals(limit, result.getRemaining());
        assertEquals(windowSeconds, result.getWindowSeconds());
        
        verify(redisTemplate).execute(any(RedisScript.class), any(), anyString(), anyString());
    }
}
