package com.example.tokenservice.account.repository;

import com.example.tokenservice.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByAccountId(UUID accountId);
    
    List<Account> findByUserId(UUID userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId")
    Optional<Account> findByAccountIdForUpdate(UUID accountId);
    
    @Query("SELECT a.balance FROM Account a WHERE a.accountId = :accountId AND a.status = 'ACTIVE'")
    Optional<BigDecimal> getBalanceByAccountId(UUID accountId);
    
    boolean existsByAccountIdAndStatus(UUID accountId, String status);
}
