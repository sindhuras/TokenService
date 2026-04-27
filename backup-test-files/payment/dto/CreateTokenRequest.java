package com.example.tokenservice.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateTokenRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotBlank(message = "Payment data is required")
    private String paymentData;
    
    @NotBlank(message = "Data type is required")
    private String dataType; // CARD, UPI, BANK
}
