package com.example.tokenservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVault {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "token_id", unique = true, nullable = false, length = 64)
    private String tokenId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "token_value", nullable = false, length = 1000)
    private String tokenValue;
    
    @Column(name = "encrypted_data", nullable = false, columnDefinition = "TEXT")
    private String encryptedData;
    
    @Column(name = "masked_data", nullable = false, length = 100)
    private String maskedData;
    
    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType; // CARD, UPI, BANK
    
    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;
    
    @Column(nullable = false, length = 20)
    private String status; // ACTIVE, EXPIRED, REVOKED
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "usage_count")
    private Integer usageCount = 0;
    
    @Column(name = "max_usage_limit")
    private Integer maxUsageLimit;
}
