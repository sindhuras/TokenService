package com.example.tokenservice.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenDataResponse {
    private String decryptedData;
}
