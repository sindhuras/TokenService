package com.example.tokenservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Token Service API is running");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> endpoints() {
        Map<String, Object> response = new HashMap<>();
        response.put("available_endpoints", Map.of(
            "Auth", "/api/auth/*",
            "Payment Tokens", "/api/tokens/*", 
            "Accounts", "/api/accounts/*",
            "Payments", "/api/payments/*",
            "Ledger", "/api/ledger/*",
            "Fraud", "/api/fraud/*"
        ));
        response.put("status", "available");
        return ResponseEntity.ok(response);
    }
}
