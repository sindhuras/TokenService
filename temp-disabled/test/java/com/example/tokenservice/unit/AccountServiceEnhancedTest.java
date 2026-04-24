package com.example.tokenservice.unit;

import com.example.tokenservice.account.entity.Account;
import com.example.tokenservice.account.entity.BalanceLock;
import com.example.tokenservice.account.repository.AccountRepository;
import com.example.tokenservice.account.repository.BalanceLockRepository;
import com.example.tokenservice.account.service.AccountService;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.common.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

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
public class AccountServiceEnhancedTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private BalanceLockRepository balanceLockRepository;
    
    @InjectMocks
    private AccountService accountService;
    
    private UUID accountId;
    private UUID userId;
    private Account account;
    private BalanceLock balanceLock;
    
    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        account = Account.builder()
                .id(1L)
                .accountId(accountId)
                .userId(userId)
                .balance(new BigDecimal("1000.00"))
                .accountType("SAVINGS")
                .currency("USD")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        
        balanceLock = BalanceLock.builder()
                .id(1L)
                .lockId("lock_123")
                .accountId(accountId)
                .amount(new BigDecimal("300.00"))
                .status("ACTIVE")
                .expiryTime(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testCreateAccountSuccess() {
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        
        Account result = accountService.createAccount(userId, "SAVINGS", "USD");
        
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("SAVINGS", result.getAccountType());
        assertEquals("USD", result.getCurrency());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    void testCreateAccountAccountExists() {
        when(accountRepository.existsByAccountIdAndStatus(accountId, "ACTIVE")).thenReturn(true);
        
        assertThrows(PaymentException.class, () -> accountService.createAccount(userId, "SAVINGS", "USD"));
        verify(accountRepository).existsByAccountIdAndStatus(accountId, "ACTIVE");
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testGetAccountSuccess() {
        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));
        
        Account result = accountService.getAccount(accountId);
        
        assertEquals(account, result);
        verify(accountRepository).findByAccountId(accountId);
    }
    
    @Test
    void testGetAccountNotFound() {
        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> accountService.getAccount(accountId));
        verify(accountRepository).findByAccountId(accountId);
    }
    
    @Test
    void testGetBalanceSuccess() {
        when(accountRepository.getBalanceByAccountId(accountId)).thenReturn(Optional.of(new BigDecimal("1000.00")));
        
        BigDecimal balance = accountService.getBalance(accountId);
        
        assertEquals(new BigDecimal("1000.00"), balance);
        verify(accountRepository).getBalanceByAccountId(accountId);
    }
    
    @Test
    void testGetBalanceNotFound() {
        when(accountRepository.getBalanceByAccountId(accountId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> accountService.getBalance(accountId));
        verify(accountRepository).getBalanceByAccountId(accountId);
    }
    
    @Test
    void testGetUserAccounts() {
        List<Account> expectedAccounts = List.of(account);
        when(accountRepository.findByUserId(userId)).thenReturn(expectedAccounts);
        
        List<Account> result = accountService.getUserAccounts(userId);
        
        assertEquals(expectedAccounts, result);
        verify(accountRepository).findByUserId(userId);
    }
    
    @Test
    void testDepositSuccess() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        
        Account result = accountService.deposit(accountId, depositAmount);
        
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
        verify(accountRepository).findByAccountIdForUpdate(accountId);
        verify(accountRepository).save(account);
    }
    
    @Test
    void testDepositNegativeAmount() {
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        
        assertThrows(PaymentException.class, () -> accountService.deposit(accountId, negativeAmount));
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testDepositZeroAmount() {
        BigDecimal zeroAmount = BigDecimal.ZERO;
        
        assertThrows(PaymentException.class, () -> accountService.deposit(accountId, zeroAmount));
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testDepositAccountNotFound() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> accountService.deposit(accountId, depositAmount));
        verify(accountRepository).findByAccountIdForUpdate(accountId);
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testDepositAccountInactive() {
        account.setStatus("INACTIVE");
        BigDecimal depositAmount = new BigDecimal("500.00");
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        
        assertThrows(PaymentException.class, () -> accountService.deposit(accountId, depositAmount));
        verify(accountRepository).findByAccountIdForUpdate(accountId);
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testLockAmountSuccess() {
        BigDecimal lockAmount = new BigDecimal("300.00");
        String transactionId = "txn_123";
        
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(balanceLockRepository.getLockedAmountByAccount(accountId, LocalDateTime.now())).thenReturn(BigDecimal.ZERO);
        when(balanceLockRepository.save(any(BalanceLock.class))).thenReturn(balanceLock);
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            
            BalanceLock result = accountService.lockAmount(accountId, lockAmount, transactionId);
            
            assertNotNull(result);
            assertEquals(accountId, result.getAccountId());
            assertEquals(lockAmount, result.getAmount());
            assertEquals("ACTIVE", result.getStatus());
            assertEquals(transactionId, result.getTransactionId());
            verify(accountRepository).findByAccountIdForUpdate(accountId);
            verify(balanceLockRepository).getLockedAmountByAccount(accountId, LocalDateTime.now());
            verify(balanceLockRepository).save(any(BalanceLock.class));
        }
    }
    
    @Test
    void testLockAmountInsufficientBalance() {
        BigDecimal lockAmount = new BigDecimal("1200.00"); // More than balance
        String transactionId = "txn_123";
        
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(balanceLockRepository.getLockedAmountByAccount(accountId, LocalDateTime.now())).thenReturn(BigDecimal.ZERO);
        
        assertThrows(PaymentException.class, () -> accountService.lockAmount(accountId, lockAmount, transactionId));
        verify(accountRepository).findByAccountIdForUpdate(accountId);
        verify(balanceLockRepository).getLockedAmountByAccount(accountId, LocalDateTime.now());
        verify(balanceLockRepository, never()).save(any());
    }
    
    @Test
    void testLockAmountNegativeAmount() {
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        String transactionId = "txn_123";
        
        assertThrows(PaymentException.class, () -> accountService.lockAmount(accountId, negativeAmount, transactionId));
        verify(accountRepository, never()).findByAccountIdForUpdate(accountId);
        verify(balanceLockRepository, never()).save(any());
    }
    
    @Test
    void testLockAmountAccountNotFound() {
        BigDecimal lockAmount = new BigDecimal("300.00");
        String transactionId = "txn_123";
        
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> accountService.lockAmount(accountId, lockAmount, transactionId));
        verify(accountRepository).findByAccountIdForUpdate(accountId);
        verify(balanceLockRepository, never()).save(any());
    }
    
    @Test
    void testReleaseLockSuccess() {
        String lockId = "lock_123";
        
        when(balanceLockRepository.findByLockId(lockId)).thenReturn(Optional.of(balanceLock));
        when(balanceLockRepository.save(any(BalanceLock.class))).thenReturn(balanceLock);
        
        accountService.releaseLock(lockId);
        
        assertEquals("RELEASED", balanceLock.getStatus());
        assertNotNull(balanceLock.getReleasedAt());
        verify(balanceLockRepository).findByLockId(lockId);
        verify(balanceLockRepository).save(balanceLock);
    }
    
    @Test
    void testReleaseLockNotFound() {
        String lockId = "nonexistent";
        
        when(balanceLockRepository.findByLockId(lockId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> accountService.releaseLock(lockId));
        verify(balanceLockRepository).findByLockId(lockId);
        verify(balanceLockRepository, never()).save(any());
    }
    
    @Test
    void testReleaseLockNotActive() {
        String lockId = "lock_123";
        balanceLock.setStatus("RELEASED");
        
        when(balanceLockRepository.findByLockId(lockId)).thenReturn(Optional.of(balanceLock));
        
        assertThrows(PaymentException.class, () -> accountService.releaseLock(lockId));
        verify(balanceLockRepository).findByLockId(lockId);
        verify(balanceLockRepository, never()).save(any());
    }
    
    @Test
    void testDebitSuccess() {
        BigDecimal debitAmount = new BigDecimal("300.00");
        
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        
        Account result = accountService.debit(accountId, debitAmount);
        
        assertEquals(new BigDecimal("700.00"), result.getBalance());
        verify(accountRepository).findByAccountIdForUpdate(accountId);
        verify(accountRepository).save(account);
    }
    
    @Test
    void testDebitInsufficientBalance() {
        BigDecimal debitAmount = new BigDecimal("1200.00"); // More than balance
        
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        
        assertThrows(PaymentException.class, () -> accountService.debit(accountId, debitAmount));
        verify(accountRepository).findByAccountIdForUpdate(accountId);
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testDebitNegativeAmount() {
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        
        assertThrows(PaymentException.class, () -> accountService.debit(accountId, negativeAmount));
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testCreditSuccess() {
        BigDecimal creditAmount = new BigDecimal("500.00");
        
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        
        Account result = accountService.credit(accountId, creditAmount);
        
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
        verify(accountRepository).findByAccountIdForUpdate(accountId);
        verify(accountRepository).save(account);
    }
    
    @Test
    void testCreditNegativeAmount() {
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        
        assertThrows(PaymentException.class, () -> accountService.credit(accountId, negativeAmount));
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testConsumeLockSuccess() {
        String lockId = "lock_123";
        
        when(balanceLockRepository.findByLockId(lockId)).thenReturn(Optional.of(balanceLock));
        when(balanceLockRepository.save(any(BalanceLock.class))).thenReturn(balanceLock);
        
        accountService.consumeLock(lockId);
        
        assertEquals("CONSUMED", balanceLock.getStatus());
        assertNotNull(balanceLock.getReleasedAt());
        verify(balanceLockRepository).findByLockId(lockId);
        verify(balanceLockRepository).save(balanceLock);
    }
    
    @Test
    void testConsumeLockNotFound() {
        String lockId = "nonexistent";
        
        when(balanceLockRepository.findByLockId(lockId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> accountService.consumeLock(lockId));
        verify(balanceLockRepository).findByLockId(lockId);
        verify(balanceLockRepository, never()).save(any());
    }
    
    @Test
    void testConsumeLockNotActive() {
        String lockId = "lock_123";
        balanceLock.setStatus("CONSUMED");
        
        when(balanceLockRepository.findByLockId(lockId)).thenReturn(Optional.of(balanceLock));
        
        assertThrows(PaymentException.class, () -> accountService.consumeLock(lockId));
        verify(balanceLockRepository).findByLockId(lockId);
        verify(balanceLockRepository, never()).save(any());
    }
    
    @Test
    void testCleanupExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        
        accountService.cleanupExpiredLocks();
        
        verify(balanceLockRepository).deleteByExpiryTimeBefore(now);
        verify(balanceLockRepository).deleteByStatusAndExpiryTimeBefore("RELEASED", now.minusDays(7));
        verify(balanceLockRepository).deleteByStatusAndExpiryTimeBefore("CONSUMED", now.minusDays(7));
    }
}
