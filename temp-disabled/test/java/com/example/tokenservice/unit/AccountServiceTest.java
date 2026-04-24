package com.example.tokenservice.unit;

import com.example.tokenservice.account.entity.Account;
import com.example.tokenservice.account.repository.AccountRepository;
import com.example.tokenservice.account.repository.BalanceLockRepository;
import com.example.tokenservice.account.service.AccountService;
import com.example.tokenservice.common.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private BalanceLockRepository balanceLockRepository;
    
    @InjectMocks
    private AccountService accountService;
    
    private UUID accountId;
    private UUID userId;
    private Account account;
    
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
                .build();
    }
    
    @Test
    void testCreateAccount() {
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
    void testDeposit() {
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        
        BigDecimal depositAmount = new BigDecimal("500.00");
        Account result = accountService.deposit(accountId, depositAmount);
        
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
        verify(accountRepository).save(account);
    }
    
    @Test
    void testDepositNegativeAmount() {
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        
        assertThrows(PaymentException.class, () -> accountService.deposit(accountId, negativeAmount));
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testDebit() {
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        
        BigDecimal debitAmount = new BigDecimal("300.00");
        Account result = accountService.debit(accountId, debitAmount);
        
        assertEquals(new BigDecimal("700.00"), result.getBalance());
        verify(accountRepository).save(account);
    }
    
    @Test
    void testDebitInsufficientBalance() {
        when(accountRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(account));
        
        BigDecimal largeAmount = new BigDecimal("2000.00");
        
        assertThrows(PaymentException.class, () -> accountService.debit(accountId, largeAmount));
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void testGetBalance() {
        when(accountRepository.getBalanceByAccountId(accountId)).thenReturn(Optional.of(new BigDecimal("1000.00")));
        
        BigDecimal balance = accountService.getBalance(accountId);
        
        assertEquals(new BigDecimal("1000.00"), balance);
        verify(accountRepository).getBalanceByAccountId(accountId);
    }
    
    @Test
    void testGetAccountNotFound() {
        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
        
        assertThrows(PaymentException.class, () -> accountService.getAccount(accountId));
    }
}
