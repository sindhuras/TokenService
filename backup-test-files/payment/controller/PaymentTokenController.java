package com.example.tokenservice.payment.controller;

import com.example.tokenservice.model.PaymentToken;
import com.example.tokenservice.payment.dto.CreateTokenRequest;
import com.example.tokenservice.payment.dto.TokenDataResponse;
import com.example.tokenservice.payment.service.PaymentTokenService;
import com.example.tokenservice.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-tokens")
@RequiredArgsConstructor
public class PaymentTokenController {
    
    private final PaymentTokenService paymentTokenService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentToken>> createToken(@Valid @RequestBody CreateTokenRequest request) {
        PaymentToken token = paymentTokenService.createToken(request.getUserId(), request.getPaymentData(), request.getDataType());
        return ResponseEntity.ok(ApiResponse.success("Payment token created successfully", token));
    }
    
    @GetMapping("/{tokenId}")
    public ResponseEntity<ApiResponse<PaymentToken>> getToken(@PathVariable String tokenId) {
        PaymentToken token = paymentTokenService.getToken(tokenId);
        return ResponseEntity.ok(ApiResponse.success(token));
    }
    
    @GetMapping("/{tokenId}/decrypt")
    public ResponseEntity<ApiResponse<TokenDataResponse>> decryptToken(@PathVariable String tokenId) {
        String decryptedData = paymentTokenService.decryptToken(tokenId);
        TokenDataResponse response = new TokenDataResponse(decryptedData);
        return ResponseEntity.ok(ApiResponse.success("Token decrypted successfully", response));
    }
    
    @DeleteMapping("/{tokenId}")
    public ResponseEntity<ApiResponse<String>> revokeToken(@PathVariable String tokenId) {
        paymentTokenService.revokeToken(tokenId);
        return ResponseEntity.ok(ApiResponse.success("Payment token revoked successfully"));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentToken>>> getActiveTokens(@RequestParam UUID userId) {
        List<PaymentToken> tokens = paymentTokenService.getActiveTokens(userId);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }
}
