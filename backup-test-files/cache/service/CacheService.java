package com.example.tokenservice.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        log.debug("Cached value for key: {}", key);
    }
    
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
        log.debug("Cached value for key: {} with TTL: {}", key, ttl);
    }
    
    public Object get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        log.debug("Retrieved value for key: {}", key);
        return value;
    }
    
    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    
    public boolean delete(String key) {
        Boolean result = redisTemplate.delete(key);
        log.debug("Deleted key: {} result: {}", key, result);
        return result != null && result;
    }
    
    public boolean exists(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return result != null && result;
    }
    
    public void expire(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
        log.debug("Set expiration for key: {} TTL: {}", key, ttl);
    }
    
    public long getTtl(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
}
