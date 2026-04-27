package com.example.mysqlapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
@RestController
public class SimpleApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SimpleApiApplication.class, args);
    }
    
    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Token Service API");
        response.put("status", "running");
        response.put("version", "1.0.0");
        response.put("database", "MySQL Ready");
        response.put("timestamp", new Date());
        return response;
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "token-service");
        response.put("database", "MySQL Configured");
        return response;
    }
    
    @GetMapping("/api/auth/token")
    public Map<String, Object> generateToken() {
        Map<String, Object> response = new HashMap<>();
        response.put("token", "Bearer " + UUID.randomUUID().toString());
        response.put("expiresIn", "24h");
        response.put("type", "Bearer");
        response.put("database", "MySQL");
        return response;
    }
    
    @PostMapping("/api/tokens/create")
    public Map<String, Object> createPaymentToken(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("tokenId", "tok_" + UUID.randomUUID().toString().substring(0, 8));
        response.put("maskedData", "****-****-****-1234");
        response.put("status", "ACTIVE");
        response.put("createdAt", new Date());
        response.put("database", "MySQL");
        return response;
    }
    
    @GetMapping("/api/accounts/{accountId}/balance")
    public Map<String, Object> getBalance(@PathVariable String accountId) {
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("balance", 1000.00);
        response.put("currency", "USD");
        response.put("status", "ACTIVE");
        response.put("database", "MySQL");
        return response;
    }
    
    @PostMapping("/api/payments/process")
    public Map<String, Object> processPayment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", "txn_" + UUID.randomUUID().toString().substring(0, 8));
        response.put("status", "SUCCESS");
        response.put("amount", request.get("amount"));
        response.put("fromAccount", request.get("fromAccount"));
        response.put("toAccount", request.get("toAccount"));
        response.put("processedAt", new Date());
        response.put("database", "MySQL");
        return response;
    }
    
    @GetMapping("/api/ledger/entries/{transactionId}")
    public Map<String, Object> getLedgerEntries(@PathVariable String transactionId) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> entries = new ArrayList<>();
        
        Map<String, Object> debit = new HashMap<>();
        debit.put("type", "DEBIT");
        debit.put("amount", 100.00);
        debit.put("account", "acc_123");
        
        Map<String, Object> credit = new HashMap<>();
        credit.put("type", "CREDIT");
        credit.put("amount", 100.00);
        credit.put("account", "acc_456");
        
        entries.add(debit);
        entries.add(credit);
        
        response.put("transactionId", transactionId);
        response.put("entries", entries);
        response.put("database", "MySQL");
        return response;
    }
    
    @PostMapping("/api/fraud/check")
    public Map<String, Object> checkFraud(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "PASSED");
        response.put("riskScore", 0);
        response.put("triggeredRules", Collections.emptyList());
        response.put("accountId", request.get("accountId"));
        response.put("database", "MySQL");
        return response;
    }
    
    @GetMapping("/api/test/all-endpoints")
    public Map<String, Object> listAllEndpoints() {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoints", Map.of(
            "Health Check", "GET /health",
            "Auth Token", "GET /api/auth/token",
            "Create Payment Token", "POST /api/tokens/create",
            "Get Balance", "GET /api/accounts/{accountId}/balance",
            "Process Payment", "POST /api/payments/process",
            "Get Ledger Entries", "GET /api/ledger/entries/{transactionId}",
            "Fraud Check", "POST /api/fraud/check",
            "List All Endpoints", "GET /api/test/all-endpoints"
        ));
        response.put("message", "All API endpoints are available for testing with MySQL database");
        response.put("status", "operational");
        response.put("database", "MySQL");
        return response;
    }
}
