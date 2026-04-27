package com.example.tokenservice.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // Lua script for atomic rate limiting
    private static final String RATE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local current = redis.call('GET', key)
        
        if current == false then
            redis.call('SET', key, 1)
            redis.call('EXPIRE', key, window)
            return {1, limit - 1}
        end
        
        local count = tonumber(current)
        if count < limit then
            local newCount = redis.call('INCR', key)
            return {newCount, limit - newCount}
        else
            return {count, 0}
        end
        """;
    
    public RateLimitResult checkRateLimit(String key, int limit, int windowSeconds) {
        RedisScript<Object[]> script = RedisScript.of(RATE_LIMIT_SCRIPT, Object[].class);
        Object[] result = redisTemplate.execute(script, Arrays.asList(key), String.valueOf(limit), String.valueOf(windowSeconds));
        
        if (result != null && result.length >= 2) {
            int currentCount = Integer.parseInt(result[0].toString());
            int remaining = Integer.parseInt(result[1].toString());
            
            boolean allowed = currentCount <= limit;
            log.debug("Rate limit check for key: {} allowed: {} current: {} remaining: {}", 
                    key, allowed, currentCount, remaining);
            
            return new RateLimitResult(allowed, currentCount, remaining, windowSeconds);
        }
        
        // Fallback to simple check if script execution fails
        return simpleRateLimitCheck(key, limit, windowSeconds);
    }
    
    private RateLimitResult simpleRateLimitCheck(String key, int limit, int windowSeconds) {
        try {
            String current = redisTemplate.opsForValue().get(key);
            
            if (current == null) {
                redisTemplate.opsForValue().set(key, "1", windowSeconds, TimeUnit.SECONDS);
                return new RateLimitResult(true, 1, limit - 1, windowSeconds);
            }
            
            int count = Integer.parseInt(current);
            if (count < limit) {
                Long newCount = redisTemplate.opsForValue().increment(key);
                return new RateLimitResult(true, newCount.intValue(), limit - newCount.intValue(), windowSeconds);
            } else {
                return new RateLimitResult(false, count, 0, windowSeconds);
            }
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            // Allow request on failure
            return new RateLimitResult(true, 0, limit, windowSeconds);
        }
    }
    
    public void resetRateLimit(String key) {
        redisTemplate.delete(key);
        log.debug("Reset rate limit for key: {}", key);
    }
    
    public RateLimitResult checkApiRateLimit(String userId, String endpoint) {
        String key = "rate_limit:api:" + userId + ":" + endpoint;
        return checkRateLimit(key, 100, 3600); // 100 requests per hour per endpoint
    }
    
    public RateLimitResult checkPaymentRateLimit(String userId) {
        String key = "rate_limit:payment:" + userId;
        return checkRateLimit(key, 10, 60); // 10 payments per minute per user
    }
    
    public RateLimitResult checkAuthRateLimit(String identifier) {
        String key = "rate_limit:auth:" + identifier;
        return checkRateLimit(key, 5, 300); // 5 auth attempts per 5 minutes
    }
    
    public static class RateLimitResult {
        private final boolean allowed;
        private final int currentCount;
        private final int remaining;
        private final int windowSeconds;
        
        public RateLimitResult(boolean allowed, int currentCount, int remaining, int windowSeconds) {
            this.allowed = allowed;
            this.currentCount = currentCount;
            this.remaining = remaining;
            this.windowSeconds = windowSeconds;
        }
        
        public boolean isAllowed() { return allowed; }
        public int getCurrentCount() { return currentCount; }
        public int getRemaining() { return remaining; }
        public int getWindowSeconds() { return windowSeconds; }
    }
}
