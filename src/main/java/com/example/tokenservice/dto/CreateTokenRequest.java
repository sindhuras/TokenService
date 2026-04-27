package com.example.tokenservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class CreateTokenRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotBlank(message = "Payment data is required")
    @Size(max = 1000, message = "Payment data cannot exceed 1000 characters")
    private String paymentData;
    
    @NotBlank(message = "Data type is required")
    @Pattern(regexp = "^(CARD|UPI|BANK)$", message = "Data type must be CARD, UPI, or BANK")
    private String dataType;
    
    private LocalDateTime expiryTime;
    
    private Integer maxUsageLimit;
}
