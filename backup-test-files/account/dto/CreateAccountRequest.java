package com.example.tokenservice.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateAccountRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotBlank(message = "Account type is required")
    private String accountType; // SAVINGS, CURRENT, BUSINESS
    
    @NotBlank(message = "Currency is required")
    private String currency; // USD, EUR, INR, etc.
}
