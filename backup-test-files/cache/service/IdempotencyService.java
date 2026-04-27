package com.example.tokenservice.cache.service;

import com.example.tokenservice.common.dto.ApiResponse;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public void storeResponse(String idempotencyKey, ApiResponse<?> response) {
        String key = "idempotency:" + idempotencyKey;
        redisTemplate.opsForValue().set(key, serializeResponse(response), Duration.ofHours(24));
        log.debug("Stored idempotent response for key: {}", idempotencyKey);
    }
    
    public ApiResponse<?> getStoredResponse(String idempotencyKey) {
        String key = "idempotency:" + idempotencyKey;
        String serializedResponse = redisTemplate.opsForValue().get(key);
        
        if (serializedResponse != null) {
            log.debug("Retrieved idempotent response for key: {}", idempotencyKey);
            return deserializeResponse(serializedResponse);
        }
        
        return null;
    }
    
    public void storeTransactionId(String idempotencyKey, String transactionId) {
        String key = "idempotency:txn:" + idempotencyKey;
        redisTemplate.opsForValue().set(key, transactionId, Duration.ofHours(24));
        log.debug("Stored transaction mapping for idempotency key: {}", idempotencyKey);
    }
    
    public String getTransactionId(String idempotencyKey) {
        String key = "idempotency:txn:" + idempotencyKey;
        String transactionId = redisTemplate.opsForValue().get(key);
        log.debug("Retrieved transaction mapping for idempotency key: {} -> {}", idempotencyKey, transactionId);
        return transactionId;
    }
    
    public boolean hasProcessed(String idempotencyKey) {
        String key = "idempotency:" + idempotencyKey;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }
    
    public void markProcessing(String idempotencyKey, Duration timeout) {
        String key = "idempotency:processing:" + idempotencyKey;
        redisTemplate.opsForValue().set(key, "true", timeout);
        log.debug("Marked request as processing for key: {}", idempotencyKey);
    }
    
    public boolean isProcessing(String idempotencyKey) {
        String key = "idempotency:processing:" + idempotencyKey;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }
    
    public void completeProcessing(String idempotencyKey) {
        String key = "idempotency:processing:" + idempotencyKey;
        redisTemplate.delete(key);
        log.debug("Completed processing for key: {}", idempotencyKey);
    }
    
    public void cleanupExpiredKeys() {
        // Redis automatically expires keys, but we can clean up any remaining processing markers
        // This would typically be called by a scheduled job
        log.debug("Cleaning up expired idempotency keys");
    }
    
    private String serializeResponse(ApiResponse<?> response) {
        try {
            // Simple serialization - in production, you'd use proper JSON serialization
            return String.format("{\"success\":%s,\"message\":\"%s\",\"data\":\"%s\"}", 
                    response.isSuccess(), response.getMessage(), response.getData());
        } catch (Exception e) {
            log.error("Failed to serialize response", e);
            throw new PaymentException("SERIALIZATION_ERROR", "Failed to serialize response");
        }
    }
    
    private ApiResponse<?> deserializeResponse(String serializedResponse) {
        try {
            // Simple deserialization - in production, you'd use proper JSON deserialization
            return ApiResponse.success("Idempotent response", serializedResponse);
        } catch (Exception e) {
            log.error("Failed to deserialize response", e);
            throw new PaymentException("DESERIALIZATION_ERROR", "Failed to deserialize response");
        }
    }
}
