package com.example.tokenservice.controller;

import com.example.tokenservice.dto.CreateTokenRequest;
import com.example.tokenservice.dto.TokenResponse;
import com.example.tokenservice.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
@Tag(name = "Token Management", description = "APIs for managing payment tokens")
public class TokenController {
    
    private final TokenService tokenService;
    
    @PostMapping
    @Operation(summary = "Create a new payment token", description = "Creates a secure token for storing payment information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Token created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "429", description = "Too many tokens for user")
    })
    public ResponseEntity<TokenResponse> createToken(@Valid @RequestBody CreateTokenRequest request) {
        log.info("Creating token for user: {}", request.getUserId());
        TokenResponse response = tokenService.createToken(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{tokenId}")
    @Operation(summary = "Get token details", description = "Retrieves token information by token ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Token not found")
    })
    public ResponseEntity<TokenResponse> getToken(
            @Parameter(description = "Token ID") @PathVariable String tokenId) {
        log.debug("Retrieving token: {}", tokenId);
        TokenResponse response = tokenService.getToken(tokenId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user tokens", description = "Retrieves all active tokens for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tokens retrieved successfully")
    })
    public ResponseEntity<List<TokenResponse>> getUserTokens(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        log.debug("Retrieving tokens for user: {}", userId);
        List<TokenResponse> responses = tokenService.getUserTokens(userId);
        return ResponseEntity.ok(responses);
    }
    
    @DeleteMapping("/{tokenId}")
    @Operation(summary = "Revoke token", description = "Revokes a token, making it inactive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Token revoked successfully"),
        @ApiResponse(responseCode = "404", description = "Token not found")
    })
    public ResponseEntity<Void> revokeToken(
            @Parameter(description = "Token ID") @PathVariable String tokenId) {
        log.info("Revoking token: {}", tokenId);
        tokenService.revokeToken(tokenId);
        return ResponseEntity.noContent().build();
    }
}
