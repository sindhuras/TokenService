package com.example.tokenservice.integration;

import com.example.tokenservice.account.service.AccountService;
import com.example.tokenservice.auth.service.AuthService;
import com.example.tokenservice.payment.service.PaymentTokenService;
import com.example.tokenservice.transaction.service.PaymentService;
import com.example.tokenservice.transaction.dto.ProcessPaymentRequest;
import com.example.tokenservice.account.dto.CreateAccountRequest;
import com.example.tokenservice.payment.dto.CreateTokenRequest;
import com.example.tokenservice.account.dto.AmountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PaymentFlowIntegrationTest {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private PaymentTokenService paymentTokenService;
    
    @Autowired
    private PaymentService paymentService;
    
    private UUID userId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private String apiToken;
    private String paymentTokenId;
    
    @BeforeEach
    void setUp() {
        // Create test user
        userId = UUID.randomUUID();
        authService.createUser("test@example.com", "password123");
        apiToken = authService.generateApiToken(userId);
        
        // Create test accounts
        fromAccountId = accountService.createAccount(userId, "SAVINGS", "USD").getAccountId();
        toAccountId = accountService.createAccount(userId, "SAVINGS", "USD").getAccountId();
        
        // Deposit funds
        accountService.deposit(fromAccountId, new BigDecimal("1000.00"));
        accountService.deposit(toAccountId, new BigDecimal("500.00"));
        
        // Create payment token
        paymentTokenId = paymentTokenService.createToken(userId, "4111111111111111", "CARD").getTokenId();
    }
    
    @Test
    void testCompletePaymentFlow() {
        // Create payment request
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setFromAccount(fromAccountId);
        request.setToAccount(toAccountId);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setPaymentTokenId(paymentTokenId);
        request.setIdempotencyKey("test_payment_" + System.currentTimeMillis());
        
        // Process payment
        var transaction = paymentService.processPayment(request);
        
        // Verify transaction
        assertNotNull(transaction);
        assertEquals("SUCCESS", transaction.getStatus());
        assertEquals(fromAccountId, transaction.getFromAccount());
        assertEquals(toAccountId, transaction.getToAccount());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        
        // Verify account balances
        BigDecimal fromBalance = accountService.getBalance(fromAccountId);
        BigDecimal toBalance = accountService.getBalance(toAccountId);
        
        assertEquals(new BigDecimal("900.00"), fromBalance);
        assertEquals(new BigDecimal("600.00"), toBalance);
    }
    
    @Test
    void testIdempotency() {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setFromAccount(fromAccountId);
        request.setToAccount(toAccountId);
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("USD");
        request.setPaymentTokenId(paymentTokenId);
        request.setIdempotencyKey("idempotent_test");
        
        // First request
        var transaction1 = paymentService.processPayment(request);
        
        // Second request with same idempotency key
        var transaction2 = paymentService.processPayment(request);
        
        // Should return same transaction
        assertEquals(transaction1.getTransactionId(), transaction2.getTransactionId());
        assertEquals(transaction1.getStatus(), transaction2.getStatus());
    }
    
    @Test
    void testInsufficientBalance() {
        // Try to transfer more than available
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setFromAccount(fromAccountId);
        request.setToAccount(toAccountId);
        request.setAmount(new BigDecimal("2000.00")); // More than balance
        request.setCurrency("USD");
        request.setPaymentTokenId(paymentTokenId);
        request.setIdempotencyKey("insufficient_test");
        
        // Should fail
        assertThrows(Exception.class, () -> paymentService.processPayment(request));
    }
    
    @Test
    void testBalanceLocking() {
        // Lock amount
        var lock = accountService.lockAmount(fromAccountId, new BigDecimal("300.00"), "test_txn");
        
        assertNotNull(lock);
        assertEquals("ACTIVE", lock.getStatus());
        assertEquals(new BigDecimal("300.00"), lock.getAmount());
        
        // Try to use locked amount
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setFromAccount(fromAccountId);
        request.setToAccount(toAccountId);
        request.setAmount(new BigDecimal("800.00")); // Available: 1000 - 300 = 700
        request.setCurrency("USD");
        request.setPaymentTokenId(paymentTokenId);
        request.setIdempotencyKey("lock_test");
        
        assertThrows(Exception.class, () -> paymentService.processPayment(request));
        
        // Release lock
        accountService.releaseLock(lock.getLockId());
        
        // Now should work
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("lock_test_after");
        
        var transaction = paymentService.processPayment(request);
        assertEquals("SUCCESS", transaction.getStatus());
    }
}
