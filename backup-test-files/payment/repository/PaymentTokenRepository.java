package com.example.tokenservice.payment.repository;

import com.example.tokenservice.model.PaymentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTokenRepository extends JpaRepository<PaymentToken, Long> {
    
    Optional<PaymentToken> findByTokenId(String tokenId);
    
    List<PaymentToken> findByUserIdAndStatus(UUID userId, String status);
    
    @Query("SELECT t FROM PaymentToken t WHERE t.userId = :userId AND t.status = 'ACTIVE' AND (t.expiryTime IS NULL OR t.expiryTime > :now)")
    List<PaymentToken> findActiveTokensByUserId(UUID userId, LocalDateTime now);
    
    void deleteByExpiryTimeBefore(LocalDateTime dateTime);
}
