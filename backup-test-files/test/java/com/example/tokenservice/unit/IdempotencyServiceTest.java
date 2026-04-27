package com.example.tokenservice.unit;

import com.example.tokenservice.cache.service.IdempotencyService;
import com.example.tokenservice.common.dto.ApiResponse;
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
public class IdempotencyServiceTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @InjectMocks
    private IdempotencyService idempotencyService;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void testStoreResponse() {
        String idempotencyKey = "test_key";
        ApiResponse<String> response = ApiResponse.success("test data");
        
        idempotencyService.storeResponse(idempotencyKey, response);
        
        verify(valueOperations).set(eq("idempotency:" + idempotencyKey), anyString(), eq(Duration.ofHours(24)));
    }
    
    @Test
    void testGetStoredResponse() {
        String idempotencyKey = "test_key";
        String serializedResponse = "{\"success\":true,\"message\":\"test data\",\"data\":\"test data\"}";
        
        when(valueOperations.get("idempotency:" + idempotencyKey)).thenReturn(serializedResponse);
        
        ApiResponse<?> result = idempotencyService.getStoredResponse(idempotencyKey);
        
        assertNotNull(result);
        assertEquals("Idempotent response", result.getMessage());
        verify(valueOperations).get("idempotency:" + idempotencyKey);
    }
    
    @Test
    void testGetStoredResponseNull() {
        String idempotencyKey = "nonexistent_key";
        
        when(valueOperations.get("idempotency:" + idempotencyKey)).thenReturn(null);
        
        ApiResponse<?> result = idempotencyService.getStoredResponse(idempotencyKey);
        
        assertNull(result);
        verify(valueOperations).get("idempotency:" + idempotencyKey);
    }
    
    @Test
    void testStoreTransactionId() {
        String idempotencyKey = "test_key";
        String transactionId = "txn_123";
        
        idempotencyService.storeTransactionId(idempotencyKey, transactionId);
        
        verify(valueOperations).set("idempotency:txn:" + idempotencyKey, transactionId, Duration.ofHours(24));
    }
    
    @Test
    void testGetTransactionId() {
        String idempotencyKey = "test_key";
        String transactionId = "txn_123";
        
        when(valueOperations.get("idempotency:txn:" + idempotencyKey)).thenReturn(transactionId);
        
        String result = idempotencyService.getTransactionId(idempotencyKey);
        
        assertEquals(transactionId, result);
        verify(valueOperations).get("idempotency:txn:" + idempotencyKey);
    }
    
    @Test
    void testGetTransactionIdNull() {
        String idempotencyKey = "nonexistent_key";
        
        when(valueOperations.get("idempotency:txn:" + idempotencyKey)).thenReturn(null);
        
        String result = idempotencyService.getTransactionId(idempotencyKey);
        
        assertNull(result);
        verify(valueOperations).get("idempotency:txn:" + idempotencyKey);
    }
    
    @Test
    void testHasProcessedTrue() {
        String idempotencyKey = "test_key";
        
        when(redisTemplate.hasKey("idempotency:" + idempotencyKey)).thenReturn(true);
        
        boolean result = idempotencyService.hasProcessed(idempotencyKey);
        
        assertTrue(result);
        verify(redisTemplate).hasKey("idempotency:" + idempotencyKey);
    }
    
    @Test
    void testHasProcessedFalse() {
        String idempotencyKey = "nonexistent_key";
        
        when(redisTemplate.hasKey("idempotency:" + idempotencyKey)).thenReturn(false);
        
        boolean result = idempotencyService.hasProcessed(idempotencyKey);
        
        assertFalse(result);
        verify(redisTemplate).hasKey("idempotency:" + idempotencyKey);
    }
    
    @Test
    void testHasProcessedNull() {
        String idempotencyKey = "test_key";
        
        when(redisTemplate.hasKey("idempotency:" + idempotencyKey)).thenReturn(null);
        
        boolean result = idempotencyService.hasProcessed(idempotencyKey);
        
        assertFalse(result);
        verify(redisTemplate).hasKey("idempotency:" + idempotencyKey);
    }
    
    @Test
    void testMarkProcessing() {
        String idempotencyKey = "test_key";
        Duration timeout = Duration.ofMinutes(5);
        
        idempotencyService.markProcessing(idempotencyKey, timeout);
        
        verify(valueOperations).set("idempotency:processing:" + idempotencyKey, "true", timeout);
    }
    
    @Test
    void testIsProcessingTrue() {
        String idempotencyKey = "test_key";
        
        when(redisTemplate.hasKey("idempotency:processing:" + idempotencyKey)).thenReturn(true);
        
        boolean result = idempotencyService.isProcessing(idempotencyKey);
        
        assertTrue(result);
        verify(redisTemplate).hasKey("idempotency:processing:" + idempotencyKey);
    }
    
    @Test
    void testIsProcessingFalse() {
        String idempotencyKey = "test_key";
        
        when(redisTemplate.hasKey("idempotency:processing:" + idempotencyKey)).thenReturn(false);
        
        boolean result = idempotencyService.isProcessing(idempotencyKey);
        
        assertFalse(result);
        verify(redisTemplate).hasKey("idempotency:processing:" + idempotencyKey);
    }
    
    @Test
    void testIsProcessingNull() {
        String idempotencyKey = "test_key";
        
        when(redisTemplate.hasKey("idempotency:processing:" + idempotencyKey)).thenReturn(null);
        
        boolean result = idempotencyService.isProcessing(idempotencyKey);
        
        assertFalse(result);
        verify(redisTemplate).hasKey("idempotency:processing:" + idempotencyKey);
    }
    
    @Test
    void testCompleteProcessing() {
        String idempotencyKey = "test_key";
        
        when(redisTemplate.delete("idempotency:processing:" + idempotencyKey)).thenReturn(true);
        
        idempotencyService.completeProcessing(idempotencyKey);
        
        verify(redisTemplate).delete("idempotency:processing:" + idempotencyKey);
    }
    
    @Test
    void testCleanupExpiredKeys() {
        idempotencyService.cleanupExpiredKeys();
        
        // This method currently just logs, so we verify it doesn't throw exceptions
        // In a real implementation, we might add actual cleanup logic
    }
}
