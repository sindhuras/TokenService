package com.example.tokenservice.unit;

import com.example.tokenservice.account.entity.BalanceLock;
import com.example.tokenservice.account.service.AccountService;
import com.example.tokenservice.fraud.service.FraudService;
import com.example.tokenservice.ledger.service.LedgerService;
import com.example.tokenservice.model.PaymentToken;
import com.example.tokenservice.payment.service.PaymentTokenService;
import com.example.tokenservice.transaction.entity.Transaction;
import com.example.tokenservice.transaction.repository.TransactionRepository;
import com.example.tokenservice.transaction.service.PaymentService;
import com.example.tokenservice.transaction.dto.ProcessPaymentRequest;
import com.example.tokenservice.common.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AccountService accountService;
    
    @Mock
    private PaymentTokenService paymentTokenService;
    
    @Mock
    private FraudService fraudService;
    
    @Mock
    private LedgerService ledgerService;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @InjectMocks
    private PaymentService paymentService;
    
    private UUID fromAccountId;
    private UUID toAccountId;
    private ProcessPaymentRequest request;
    private Transaction transaction;
    private PaymentToken paymentToken;
    private BalanceLock balanceLock;
    
    @BeforeEach
    void setUp() {
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();
        
        request = new ProcessPaymentRequest();
        request.setFromAccount(fromAccountId);
        request.setToAccount(toAccountId);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setPaymentTokenId("tok_123");
        request.setIdempotencyKey("idempotent_test");
        
        transaction = Transaction.builder()
                .id(1L)
                .transactionId("txn_123")
                .fromAccount(fromAccountId)
                .toAccount(toAccountId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status("PENDING")
                .idempotencyKey("idempotent_test")
                .paymentTokenId("tok_123")
                .createdAt(LocalDateTime.now())
                .build();
        
        paymentToken = PaymentToken.builder()
                .id(1L)
                .tokenId("tok_123")
                .userId(UUID.randomUUID())
                .tokenValue("tok_value")
                .encryptedData("encrypted")
                .maskedData("****-****-****-1234")
                .dataType("CARD")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        
        balanceLock = BalanceLock.builder()
                .id(1L)
                .lockId("lock_123")
                .accountId(fromAccountId)
                .amount(new BigDecimal("100.00"))
                .status("ACTIVE")
                .expiryTime(LocalDateTime.now().plusMinutes(30))
                .transactionId("txn_123")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testProcessPaymentSuccess() {
        when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
        when(paymentTokenService.getToken(request.getPaymentTokenId())).thenReturn(paymentToken);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(fraudService.checkFraud(any(UUID.class), any(BigDecimal.class), anyString())).thenReturn(true);
        when(accountService.lockAmount(any(UUID.class), any(BigDecimal.class), anyString())).thenReturn(balanceLock);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        
        Transaction result = paymentService.processPayment(request);
        
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(fromAccountId, result.getFromAccount());
        assertEquals(toAccountId, result.getToAccount());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        
        verify(transactionRepository).findByIdempotencyKey(request.getIdempotencyKey());
        verify(paymentTokenService).getToken(request.getPaymentTokenId());
        verify(fraudService).checkFraud(any(UUID.class), any(BigDecimal.class), anyString());
        verify(accountService).lockAmount(any(UUID.class), any(BigDecimal.class), anyString());
        verify(accountService).debit(any(UUID.class), any(BigDecimal.class));
        verify(accountService).credit(any(UUID.class), any(BigDecimal.class));
        verify(ledgerService).createEntries(anyString(), any(UUID.class), any(UUID.class), any(BigDecimal.class));
        verify(accountService).consumeLock(anyString());
        verify(transactionRepository, atLeast(2)).save(any(Transaction.class));
    }
    
    @Test
    void testProcessPaymentIdempotencyKeyExists() {
        when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.of(transaction));
        
        Transaction result = paymentService.processPayment(request);
        
        assertEquals(transaction, result);
        verify(transactionRepository).findByIdempotencyKey(request.getIdempotencyKey());
        verify(paymentTokenService, never()).getToken(anyString());
        verify(fraudService, never()).checkFraud(any(UUID.class), any(BigDecimal.class), anyString());
    }
    
    @Test
    void testProcessPaymentInvalidToken() {
        when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
        paymentToken.setStatus("INACTIVE");
        when(paymentTokenService.getToken(request.getPaymentTokenId())).thenReturn(paymentToken);
        
        assertThrows(PaymentException.class, () -> paymentService.processPayment(request));
        
        verify(transactionRepository).findByIdempotencyKey(request.getIdempotencyKey());
        verify(paymentTokenService).getToken(request.getPaymentTokenId());
        verify(fraudService, never()).checkFraud(any(UUID.class), any(BigDecimal.class), anyString());
    }
    
    @Test
    void testProcessPaymentFraudCheckFailed() {
        when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
        when(paymentTokenService.getToken(request.getPaymentTokenId())).thenReturn(paymentToken);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(fraudService.checkFraud(any(UUID.class), any(BigDecimal.class), anyString())).thenReturn(false);
        
        assertThrows(PaymentException.class, () -> paymentService.processPayment(request));
        
        verify(transactionRepository).findByIdempotencyKey(request.getIdempotencyKey());
        verify(paymentTokenService).getToken(request.getPaymentTokenId());
        verify(fraudService).checkFraud(any(UUID.class), any(BigDecimal.class), anyString());
        verify(transactionRepository, atLeast(2)).save(any(Transaction.class));
    }
    
    @Test
    void testProcessPaymentLockFailure() {
        when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
        when(paymentTokenService.getToken(request.getPaymentTokenId())).thenReturn(paymentToken);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(fraudService.checkFraud(any(UUID.class), any(BigDecimal.class), anyString())).thenReturn(true);
        when(accountService.lockAmount(any(UUID.class), any(BigDecimal.class), anyString()))
                .thenThrow(new PaymentException("INSUFFICIENT_BALANCE", "Insufficient balance"));
        
        assertThrows(PaymentException.class, () -> paymentService.processPayment(request));
        
        verify(transactionRepository).findByIdempotencyKey(request.getIdempotencyKey());
        verify(paymentTokenService).getToken(request.getPaymentTokenId());
        verify(fraudService).checkFraud(any(UUID.class), any(BigDecimal.class), anyString());
        verify(accountService).lockAmount(any(UUID.class), any(BigDecimal.class), anyString());
        verify(accountService, never()).debit(any(UUID.class), any(BigDecimal.class));
        verify(accountService, never()).credit(any(UUID.class), any(BigDecimal.class));
    }
    
    @Test
    void testProcessPaymentDebitFailure() {
        when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
        when(paymentTokenService.getToken(request.getPaymentTokenId())).thenReturn(paymentToken);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(fraudService.checkFraud(any(UUID.class), any(BigDecimal.class), anyString())).thenReturn(true);
        when(accountService.lockAmount(any(UUID.class), any(BigDecimal.class), anyString())).thenReturn(balanceLock);
        when(accountService.debit(any(UUID.class), any(BigDecimal.class)))
                .thenThrow(new PaymentException("INSUFFICIENT_BALANCE", "Insufficient balance"));
        
        assertThrows(PaymentException.class, () -> paymentService.processPayment(request));
        
        verify(transactionRepository).findByIdempotencyKey(request.getIdempotencyKey());
        verify(paymentTokenService).getToken(request.getPaymentTokenId());
        verify(fraudService).checkFraud(any(UUID.class), any(BigDecimal.class), anyString());
        verify(accountService).lockAmount(any(UUID.class), any(BigDecimal.class), anyString());
        verify(accountService).debit(any(UUID.class), any(BigDecimal.class));
        verify(accountService).releaseLock(anyString());
        verify(accountService, never()).credit(any(UUID.class), any(BigDecimal.class));
    }
    
    @Test
    void testCancelPaymentSuccess() {
        String transactionId = "txn_123";
        transaction.setStatus("PENDING");
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
        
        paymentService.cancelPayment(transactionId);
        
        assertEquals("CANCELLED", transaction.getStatus());
        assertNotNull(transaction.getCompletedAt());
        verify(transactionRepository).findByTransactionId(transactionId);
        verify(accountService).releaseLock(transactionId);
        verify(transactionRepository).save(transaction);
    }
    
    @Test
    void testCancelPaymentNotFound() {
        String transactionId = "nonexistent";
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> paymentService.cancelPayment(transactionId));
        
        verify(transactionRepository).findByTransactionId(transactionId);
        verify(accountService, never()).releaseLock(anyString());
        verify(transactionRepository, never()).save(any());
    }
    
    @Test
    void testCancelPaymentInvalidStatus() {
        String transactionId = "txn_123";
        transaction.setStatus("SUCCESS");
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
        
        assertThrows(PaymentException.class, () -> paymentService.cancelPayment(transactionId));
        
        verify(transactionRepository).findByTransactionId(transactionId);
        verify(accountService, never()).releaseLock(anyString());
        verify(transactionRepository, never()).save(any());
    }
    
    @Test
    void testGetTransactionSuccess() {
        String transactionId = "txn_123";
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
        
        Transaction result = paymentService.getTransaction(transactionId);
        
        assertEquals(transaction, result);
        verify(transactionRepository).findByTransactionId(transactionId);
    }
    
    @Test
    void testGetTransactionNotFound() {
        String transactionId = "nonexistent";
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> paymentService.getTransaction(transactionId));
        
        verify(transactionRepository).findByTransactionId(transactionId);
    }
    
    @Test
    void testGetAccountTransactions() {
        UUID accountId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();
        List<Transaction> expectedTransactions = List.of(transaction);
        
        when(transactionRepository.findTransactionsByAccountAndDateRange(accountId, startDate, endDate))
                .thenReturn(expectedTransactions);
        
        List<Transaction> result = paymentService.getAccountTransactions(accountId, startDate, endDate);
        
        assertEquals(expectedTransactions, result);
        verify(transactionRepository).findTransactionsByAccountAndDateRange(accountId, startDate, endDate);
    }
    
    @Test
    void testProcessPaymentWithoutIdempotencyKey() {
        request.setIdempotencyKey(null);
        
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(paymentTokenService.getToken(request.getPaymentTokenId())).thenReturn(paymentToken);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(fraudService.checkFraud(any(UUID.class), any(BigDecimal.class), anyString())).thenReturn(true);
        when(accountService.lockAmount(any(UUID.class), any(BigDecimal.class), anyString())).thenReturn(balanceLock);
        
        Transaction result = paymentService.processPayment(request);
        
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        verify(transactionRepository).findByIdempotencyKey(anyString());
        verify(paymentTokenService).getToken(request.getPaymentTokenId());
        verify(fraudService).checkFraud(any(UUID.class), any(BigDecimal.class), anyString());
    }
}
