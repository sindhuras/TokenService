package com.example.tokenservice.account.service;

import com.example.tokenservice.account.entity.Account;
import com.example.tokenservice.account.entity.BalanceLock;
import com.example.tokenservice.account.repository.AccountRepository;
import com.example.tokenservice.account.repository.BalanceLockRepository;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.common.util.EncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final BalanceLockRepository balanceLockRepository;
    
    // Constructor
    public AccountService(AccountRepository accountRepository, BalanceLockRepository balanceLockRepository) {
        this.accountRepository = accountRepository;
        this.balanceLockRepository = balanceLockRepository;
    }
    
    @Transactional
    public Account createAccount(UUID userId, String accountType, String currency) {
        UUID accountId = UUID.randomUUID();
        
        if (accountRepository.existsByAccountIdAndStatus(accountId, "ACTIVE")) {
            throw new PaymentException("ACCOUNT_EXISTS", "Account already exists");
        }
        
        Account account = Account.builder()
                .accountId(accountId)
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .accountType(accountType)
                .currency(currency)
                .status("ACTIVE")
                .build();
        
        return accountRepository.save(account);
    }
    
    @Transactional(readOnly = true)
    public Account getAccount(UUID accountId) {
        return accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new PaymentException("ACCOUNT_NOT_FOUND", "Account not found"));
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID accountId) {
        return accountRepository.getBalanceByAccountId(accountId)
                .orElseThrow(() -> new PaymentException("ACCOUNT_NOT_FOUND", "Account not found"));
    }
    
    @Transactional(readOnly = true)
    public List<Account> getUserAccounts(UUID userId) {
        return accountRepository.findByUserId(userId);
    }
    
    @Transactional
    public Account deposit(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("INVALID_AMOUNT", "Deposit amount must be positive");
        }
        
        Account account = accountRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new PaymentException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new PaymentException("ACCOUNT_INACTIVE", "Account is not active");
        }
        
        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now());
        
        return accountRepository.save(account);
    }
    
    @Transactional
    public BalanceLock lockAmount(UUID accountId, BigDecimal amount, String transactionId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("INVALID_AMOUNT", "Lock amount must be positive");
        }
        
        Account account = accountRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new PaymentException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new PaymentException("ACCOUNT_INACTIVE", "Account is not active");
        }
        
        BigDecimal lockedAmount = balanceLockRepository.getLockedAmountByAccount(accountId, LocalDateTime.now());
        BigDecimal availableBalance = account.getBalance().subtract(lockedAmount);
        
        if (availableBalance.compareTo(amount) < 0) {
            throw new PaymentException("INSUFFICIENT_BALANCE", "Insufficient balance");
        }
        
        String lockId = EncryptionUtil.generateToken();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(30);
        
        BalanceLock balanceLock = BalanceLock.builder()
                .lockId(lockId)
                .accountId(accountId)
                .amount(amount)
                .status("ACTIVE")
                .expiryTime(expiryTime)
                .transactionId(transactionId)
                .build();
        
        return balanceLockRepository.save(balanceLock);
    }
    
    @Transactional
    public void releaseLock(String lockId) {
        BalanceLock balanceLock = balanceLockRepository.findByLockId(lockId)
                .orElseThrow(() -> new PaymentException("LOCK_NOT_FOUND", "Balance lock not found"));
        
        if (!"ACTIVE".equals(balanceLock.getStatus())) {
            throw new PaymentException("LOCK_NOT_ACTIVE", "Balance lock is not active");
        }
        
        balanceLock.setStatus("RELEASED");
        balanceLock.setReleasedAt(LocalDateTime.now());
        
        balanceLockRepository.save(balanceLock);
    }
    
    @Transactional
    public Account debit(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("INVALID_AMOUNT", "Debit amount must be positive");
        }
        
        Account account = accountRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new PaymentException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new PaymentException("ACCOUNT_INACTIVE", "Account is not active");
        }
        
        if (account.getBalance().compareTo(amount) < 0) {
            throw new PaymentException("INSUFFICIENT_BALANCE", "Insufficient balance");
        }
        
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());
        
        return accountRepository.save(account);
    }
    
    @Transactional
    public Account credit(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("INVALID_AMOUNT", "Credit amount must be positive");
        }
        
        Account account = accountRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new PaymentException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new PaymentException("ACCOUNT_INACTIVE", "Account is not active");
        }
        
        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now());
        
        return accountRepository.save(account);
    }
    
    @Transactional
    public void consumeLock(String lockId) {
        BalanceLock balanceLock = balanceLockRepository.findByLockId(lockId)
                .orElseThrow(() -> new PaymentException("LOCK_NOT_FOUND", "Balance lock not found"));
        
        if (!"ACTIVE".equals(balanceLock.getStatus())) {
            throw new PaymentException("LOCK_NOT_ACTIVE", "Balance lock is not active");
        }
        
        balanceLock.setStatus("CONSUMED");
        balanceLock.setReleasedAt(LocalDateTime.now());
        
        balanceLockRepository.save(balanceLock);
    }
    
    @Transactional
    public void cleanupExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        balanceLockRepository.deleteByExpiryTimeBefore(now);
        balanceLockRepository.deleteByStatusAndExpiryTimeBefore("RELEASED", now.minusDays(7));
        balanceLockRepository.deleteByStatusAndExpiryTimeBefore("CONSUMED", now.minusDays(7));
    }
}
