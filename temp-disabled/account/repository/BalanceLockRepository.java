package com.example.tokenservice.account.repository;

import com.example.tokenservice.account.entity.BalanceLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BalanceLockRepository extends JpaRepository<BalanceLock, Long> {
    
    Optional<BalanceLock> findByLockId(String lockId);
    
    List<BalanceLock> findByAccountIdAndStatus(UUID accountId, String status);
    
    @Query("SELECT bl FROM BalanceLock bl WHERE bl.accountId = :accountId AND bl.status = 'ACTIVE' AND bl.expiryTime > :now")
    List<BalanceLock> findActiveLocksByAccount(UUID accountId, LocalDateTime now);
    
    @Query("SELECT COALESCE(SUM(bl.amount), 0) FROM BalanceLock bl WHERE bl.accountId = :accountId AND bl.status = 'ACTIVE' AND bl.expiryTime > :now")
    java.math.BigDecimal getLockedAmountByAccount(UUID accountId, LocalDateTime now);
    
    void deleteByExpiryTimeBefore(LocalDateTime dateTime);
    
    void deleteByStatusAndExpiryTimeBefore(String status, LocalDateTime dateTime);
}
