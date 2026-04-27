package com.example.tokenservice.transaction.service;

import com.example.tokenservice.account.service.AccountService;
import com.example.tokenservice.fraud.service.FraudService;
import com.example.tokenservice.ledger.service.LedgerService;
import com.example.tokenservice.model.PaymentToken;
import com.example.tokenservice.payment.service.PaymentTokenService;
import com.example.tokenservice.transaction.entity.Transaction;
import com.example.tokenservice.transaction.repository.TransactionRepository;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.transaction.dto.ProcessPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final PaymentTokenService paymentTokenService;
    private final FraudService fraudService;
    private final LedgerService ledgerService;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Transactional
    public Transaction processPayment(ProcessPaymentRequest request) {
        log.info("Processing payment request: {}", request);
        
        // Validate idempotency
        if (request.getIdempotencyKey() != null) {
            Transaction existingTransaction = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElse(null);
            if (existingTransaction != null) {
                log.info("Returning existing transaction for idempotency key: {}", request.getIdempotencyKey());
                return existingTransaction;
            }
        }
        
        // Validate payment token
        PaymentToken paymentToken = paymentTokenService.getToken(request.getPaymentTokenId());
        if (!"ACTIVE".equals(paymentToken.getStatus())) {
            throw new PaymentException("INVALID_TOKEN", "Payment token is not active");
        }
        
        // Create transaction with PENDING status
        String transactionId = generateTransactionId();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("PENDING")
                .idempotencyKey(request.getIdempotencyKey())
                .paymentTokenId(request.getPaymentTokenId())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        try {
            // Fraud check
            boolean fraudCheckPassed = fraudService.checkFraud(request.getFromAccount(), request.getAmount(), transactionId);
            transaction.setFraudCheckResult(fraudCheckPassed ? "PASSED" : "FAILED");
            
            if (!fraudCheckPassed) {
                transaction.setStatus("FAILED");
                transaction.setFailureReason("Fraud check failed");
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
                throw new PaymentException("FRAUD_DETECTED", "Transaction flagged for fraud");
            }
            
            // Lock amount in source account
            String lockId = accountService.lockAmount(request.getFromAccount(), request.getAmount(), transactionId).getLockId();
            
            try {
                // Process debit and credit
                accountService.debit(request.getFromAccount(), request.getAmount());
                accountService.credit(request.getToAccount(), request.getAmount());
                
                // Create ledger entries
                ledgerService.createEntries(transactionId, request.getFromAccount(), request.getToAccount(), request.getAmount());
                
                // Consume the lock
                accountService.consumeLock(lockId);
                
                // Update transaction status
                transaction.setStatus("SUCCESS");
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
                
                log.info("Payment processed successfully: {}", transactionId);
                return transaction;
                
            } catch (Exception e) {
                // Release lock on failure
                accountService.releaseLock(lockId);
                throw e;
            }
            
        } catch (Exception e) {
            // Update transaction status to FAILED
            transaction.setStatus("FAILED");
            transaction.setFailureReason(e.getMessage());
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            
            log.error("Payment processing failed: {}", transactionId, e);
            throw e;
        }
    }
    
    @Transactional
    public void cancelPayment(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentException("TRANSACTION_NOT_FOUND", "Transaction not found"));
        
        if (!"PENDING".equals(transaction.getStatus())) {
            throw new PaymentException("INVALID_STATUS", "Cannot cancel transaction in status: " + transaction.getStatus());
        }
        
        // Release any locks associated with this transaction
        accountService.releaseLock(transactionId);
        
        transaction.setStatus("CANCELLED");
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        
        log.info("Payment cancelled: {}", transactionId);
    }
    
    @Transactional(readOnly = true)
    public Transaction getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentException("TRANSACTION_NOT_FOUND", "Transaction not found"));
    }
    
    @Transactional(readOnly = true)
    public java.util.List<Transaction> getAccountTransactions(UUID accountId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findTransactionsByAccountAndDateRange(accountId, startDate, endDate);
    }
    
    private String generateTransactionId() {
        return "txn_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private void cacheIdempotencyKey(String idempotencyKey, String transactionId) {
        if (idempotencyKey != null) {
            redisTemplate.opsForValue().set("idempotency:" + idempotencyKey, transactionId, 24, TimeUnit.HOURS);
        }
    }
}
