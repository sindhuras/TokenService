package com.example.tokenservice.repository;

import com.example.tokenservice.entity.TokenVault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenVaultRepository extends JpaRepository<TokenVault, Long> {
    
    Optional<TokenVault> findByTokenId(String tokenId);
    
    List<TokenVault> findByUserId(UUID userId);
    
    List<TokenVault> findByUserIdAndStatus(UUID userId, String status);
    
    @Query("SELECT t FROM TokenVault t WHERE t.expiryTime < :currentTime AND t.status = 'ACTIVE'")
    List<TokenVault> findExpiredTokens(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT t FROM TokenVault t WHERE t.status = 'ACTIVE' AND t.maxUsageLimit > 0 AND t.usageCount >= t.maxUsageLimit")
    List<TokenVault> findTokensWithUsageLimitReached();
    
    @Query("SELECT COUNT(t) FROM TokenVault t WHERE t.userId = :userId AND t.status = 'ACTIVE'")
    long countActiveTokensByUser(@Param("userId") UUID userId);
    
    void deleteByTokenId(String tokenId);
}
