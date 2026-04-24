package com.example.tokenservice.fraud.repository;

import com.example.tokenservice.fraud.entity.FraudCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, Long> {
    
    List<FraudCheck> findByAccountId(UUID accountId);
    
    List<FraudCheck> findByAccountIdAndResult(UUID accountId, String result);
    
    @Query("SELECT COUNT(fc) FROM FraudCheck fc WHERE fc.accountId = :accountId AND fc.createdAt >= :since")
    Long countChecksByAccountSince(UUID accountId, LocalDateTime since);
    
    @Query("SELECT COUNT(fc) FROM FraudCheck fc WHERE fc.accountId = :accountId AND fc.result = 'FAILED' AND fc.createdAt >= :since")
    Long countFailedChecksByAccountSince(UUID accountId, LocalDateTime since);
    
    @Query("SELECT fc FROM FraudCheck fc WHERE fc.accountId = :accountId AND fc.createdAt >= :since ORDER BY fc.createdAt DESC")
    List<FraudCheck> findRecentChecksByAccount(UUID accountId, LocalDateTime since);
}
