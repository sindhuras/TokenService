package com.example.tokenservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_tokens")
public class TokenVault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id", unique = true, nullable = false)
    private String tokenId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_value", nullable = false)
    private String tokenValue;

    @Column(name = "encrypted_data", nullable = false)
    private String encryptedData;

    @Column(name = "masked_data", nullable = false)
    private String maskedData;

    @Column(name = "data_type", nullable = false)
    private String dataType; // CARD, UPI, BANK

    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    // Constructors
    public TokenVault() {}
    
    public TokenVault(String tokenId, UUID userId, String tokenValue, String encryptedData, 
                     String maskedData, String dataType, LocalDateTime expiryTime, String status) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.tokenValue = tokenValue;
        this.encryptedData = encryptedData;
        this.maskedData = maskedData;
        this.dataType = dataType;
        this.expiryTime = expiryTime;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getTokenValue() { return tokenValue; }
    public void setTokenValue(String tokenValue) { this.tokenValue = tokenValue; }
    
    public String getEncryptedData() { return encryptedData; }
    public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }
    
    public String getMaskedData() { return maskedData; }
    public void setMaskedData(String maskedData) { this.maskedData = maskedData; }
    
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    
    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
