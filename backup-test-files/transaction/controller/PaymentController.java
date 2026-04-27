package com.example.tokenservice.transaction.controller;

import com.example.tokenservice.transaction.dto.ProcessPaymentRequest;
import com.example.tokenservice.transaction.entity.Transaction;
import com.example.tokenservice.transaction.service.PaymentService;
import com.example.tokenservice.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        Transaction transaction = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", transaction));
    }
    
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Transaction>> getTransaction(@PathVariable String transactionId) {
        Transaction transaction = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }
    
    @PostMapping("/{transactionId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelPayment(@PathVariable String transactionId) {
        paymentService.cancelPayment(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully"));
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<Transaction>>> getAccountTransactions(
            @PathVariable UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Transaction> transactions = paymentService.getAccountTransactions(accountId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}
