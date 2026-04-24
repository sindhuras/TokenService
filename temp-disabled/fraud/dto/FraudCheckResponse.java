package com.example.tokenservice.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FraudCheckResponse {
    private boolean allowed;
    private String message;
    
    public FraudCheckResponse(boolean allowed) {
        this.allowed = allowed;
        this.message = allowed ? "Transaction allowed" : "Transaction blocked";
    }
}
