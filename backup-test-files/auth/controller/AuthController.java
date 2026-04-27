package com.example.tokenservice.auth.controller;

import com.example.tokenservice.auth.dto.CreateUserRequest;
import com.example.tokenservice.auth.dto.TokenResponse;
import com.example.tokenservice.auth.service.AuthService;
import com.example.tokenservice.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<TokenResponse>> generateToken(@RequestParam UUID userId) {
        String token = authService.generateApiToken(userId);
        TokenResponse response = new TokenResponse(token);
        return ResponseEntity.ok(ApiResponse.success("Token generated successfully", response));
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> createUser(@Valid @RequestBody CreateUserRequest request) {
        authService.createUser(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("User created successfully"));
    }
    
    @PostMapping("/revoke")
    public ResponseEntity<ApiResponse<String>> revokeToken(@RequestParam String tokenId) {
        authService.revokeToken(tokenId);
        return ResponseEntity.ok(ApiResponse.success("Token revoked successfully"));
    }
    
    @GetMapping("/tokens")
    public ResponseEntity<ApiResponse<?>> getActiveTokens(@RequestParam UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(authService.getActiveTokens(userId)));
    }
}
