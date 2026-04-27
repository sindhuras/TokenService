package com.example.tokenservice.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProcessPaymentRequest {
    
    @NotNull(message = "From account is required")
    private UUID fromAccount;
    
    @NotNull(message = "To account is required")
    private UUID toAccount;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    @NotBlank(message = "Payment token ID is required")
    private String paymentTokenId;
    
    private String idempotencyKey;
}
