package com.example.tokenservice.auth.repository;

import com.example.tokenservice.auth.entity.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {
    
    Optional<ApiToken> findByTokenHash(String tokenHash);
    
    List<ApiToken> findByUserIdAndStatus(UUID userId, String status);
    
    @Query("SELECT t FROM ApiToken t WHERE t.userId = :userId AND t.status = 'ACTIVE' AND t.expiryTime > :now")
    List<ApiToken> findActiveTokensByUserId(UUID userId, LocalDateTime now);
    
    void deleteByExpiryTimeBefore(LocalDateTime dateTime);
}
