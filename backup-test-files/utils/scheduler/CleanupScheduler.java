package com.example.tokenservice.scheduler;

import com.example.tokenservice.account.service.AccountService;
import com.example.tokenservice.cache.service.IdempotencyService;
import com.example.tokenservice.payment.repository.PaymentTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {
    
    private final AccountService accountService;
    private final IdempotencyService idempotencyService;
    private final PaymentTokenRepository paymentTokenRepository;
    
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired tokens");
        try {
            // Clean up expired payment tokens
            paymentTokenRepository.deleteByExpiryTimeBefore(java.time.LocalDateTime.now());
            log.info("Completed cleanup of expired payment tokens");
        } catch (Exception e) {
            log.error("Error during cleanup of expired tokens", e);
        }
    }
    
    @Scheduled(cron = "0 30 2 * * ?") // Run at 2:30 AM every day
    public void cleanupExpiredLocks() {
        log.info("Starting cleanup of expired balance locks");
        try {
            accountService.cleanupExpiredLocks();
            log.info("Completed cleanup of expired balance locks");
        } catch (Exception e) {
            log.error("Error during cleanup of expired balance locks", e);
        }
    }
    
    @Scheduled(cron = "0 0 3 * * ?") // Run at 3 AM every day
    public void cleanupIdempotencyKeys() {
        log.info("Starting cleanup of expired idempotency keys");
        try {
            idempotencyService.cleanupExpiredKeys();
            log.info("Completed cleanup of expired idempotency keys");
        } catch (Exception e) {
            log.error("Error during cleanup of expired idempotency keys", e);
        }
    }
}
