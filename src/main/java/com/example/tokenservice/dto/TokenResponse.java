package com.example.tokenservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    
    private String tokenId;
    private UUID userId;
    private String maskedData;
    private String dataType;
    private LocalDateTime expiryTime;
    private String status;
    private LocalDateTime createdAt;
    private Integer usageCount;
    private Integer maxUsageLimit;
}
