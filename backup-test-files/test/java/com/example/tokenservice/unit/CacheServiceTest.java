package com.example.tokenservice.unit;

import com.example.tokenservice.cache.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CacheServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @InjectMocks
    private CacheService cacheService;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void testSet() {
        String key = "test_key";
        String value = "test_value";
        
        cacheService.set(key, value);
        
        verify(valueOperations).set(key, value);
    }
    
    @Test
    void testSetWithTTL() {
        String key = "test_key";
        String value = "test_value";
        Duration ttl = Duration.ofMinutes(5);
        
        cacheService.set(key, value, ttl);
        
        verify(valueOperations).set(key, value, ttl);
    }
    
    @Test
    void testGet() {
        String key = "test_key";
        String expectedValue = "test_value";
        
        when(valueOperations.get(key)).thenReturn(expectedValue);
        
        Object result = cacheService.get(key);
        
        assertEquals(expectedValue, result);
        verify(valueOperations).get(key);
    }
    
    @Test
    void testGetWithClass() {
        String key = "test_key";
        String expectedValue = "test_value";
        
        when(valueOperations.get(key)).thenReturn(expectedValue);
        
        String result = cacheService.get(key, String.class);
        
        assertEquals(expectedValue, result);
        verify(valueOperations).get(key);
    }
    
    @Test
    void testGetWithClassWrongType() {
        String key = "test_key";
        Integer wrongTypeValue = 123;
        
        when(valueOperations.get(key)).thenReturn(wrongTypeValue);
        
        String result = cacheService.get(key, String.class);
        
        assertNull(result);
        verify(valueOperations).get(key);
    }
    
    @Test
    void testGetNull() {
        String key = "nonexistent_key";
        
        when(valueOperations.get(key)).thenReturn(null);
        
        Object result = cacheService.get(key);
        
        assertNull(result);
        verify(valueOperations).get(key);
    }
    
    @Test
    void testDelete() {
        String key = "test_key";
        
        when(redisTemplate.delete(key)).thenReturn(true);
        
        boolean result = cacheService.delete(key);
        
        assertTrue(result);
        verify(redisTemplate).delete(key);
    }
    
    @Test
    void testDeleteFalse() {
        String key = "nonexistent_key";
        
        when(redisTemplate.delete(key)).thenReturn(false);
        
        boolean result = cacheService.delete(key);
        
        assertFalse(result);
        verify(redisTemplate).delete(key);
    }
    
    @Test
    void testDeleteNull() {
        String key = "test_key";
        
        when(redisTemplate.delete(key)).thenReturn(null);
        
        boolean result = cacheService.delete(key);
        
        assertFalse(result);
        verify(redisTemplate).delete(key);
    }
    
    @Test
    void testExists() {
        String key = "test_key";
        
        when(redisTemplate.hasKey(key)).thenReturn(true);
        
        boolean result = cacheService.exists(key);
        
        assertTrue(result);
        verify(redisTemplate).hasKey(key);
    }
    
    @Test
    void testNotExists() {
        String key = "nonexistent_key";
        
        when(redisTemplate.hasKey(key)).thenReturn(false);
        
        boolean result = cacheService.exists(key);
        
        assertFalse(result);
        verify(redisTemplate).hasKey(key);
    }
    
    @Test
    void testExistsNull() {
        String key = "test_key";
        
        when(redisTemplate.hasKey(key)).thenReturn(null);
        
        boolean result = cacheService.exists(key);
        
        assertFalse(result);
        verify(redisTemplate).hasKey(key);
    }
    
    @Test
    void testExpire() {
        String key = "test_key";
        Duration ttl = Duration.ofMinutes(10);
        
        cacheService.expire(key, ttl);
        
        verify(redisTemplate).expire(key, ttl);
    }
    
    @Test
    void testGetTtl() {
        String key = "test_key";
        long expectedTtl = 300L;
        
        when(redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(expectedTtl);
        
        long result = cacheService.getTtl(key);
        
        assertEquals(expectedTtl, result);
        verify(redisTemplate).getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    @Test
    void testGetTtlNegative() {
        String key = "test_key";
        
        when(redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(-1L);
        
        long result = cacheService.getTtl(key);
        
        assertEquals(-1L, result);
        verify(redisTemplate).getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    @Test
    void testGetTtlNull() {
        String key = "test_key";
        
        when(redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(null);
        
        long result = cacheService.getTtl(key);
        
        assertEquals(-1L, result);
        verify(redisTemplate).getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
    }
}
