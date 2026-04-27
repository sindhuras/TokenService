package com.example.tokenservice.ledger.controller;

import com.example.tokenservice.ledger.entity.LedgerEntry;
import com.example.tokenservice.ledger.service.LedgerService;
import com.example.tokenservice.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {
    
    private final LedgerService ledgerService;
    
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<List<LedgerEntry>>> getTransactionEntries(@PathVariable String transactionId) {
        List<LedgerEntry> entries = ledgerService.getTransactionEntries(transactionId);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<LedgerEntry>>> getAccountEntries(
            @PathVariable UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<LedgerEntry> entries = ledgerService.getAccountEntries(accountId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }
    
    @GetMapping("/account/{accountId}/balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getAccountBalance(@PathVariable UUID accountId) {
        BigDecimal balance = ledgerService.getAccountBalance(accountId);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }
    
    @GetMapping("/account/{accountId}/summary")
    public ResponseEntity<ApiResponse<AccountSummary>> getAccountSummary(@PathVariable UUID accountId) {
        BigDecimal totalDebits = ledgerService.getTotalDebitsByAccount(accountId);
        BigDecimal totalCredits = ledgerService.getTotalCreditsByAccount(accountId);
        BigDecimal balance = ledgerService.getAccountBalance(accountId);
        
        AccountSummary summary = new AccountSummary(totalDebits, totalCredits, balance);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    @GetMapping("/transaction/{transactionId}/validate")
    public ResponseEntity<ApiResponse<String>> validateDoubleEntry(@PathVariable String transactionId) {
        ledgerService.validateDoubleEntry(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Double-entry validation passed"));
    }
    
    private static class AccountSummary {
        private final BigDecimal totalDebits;
        private final BigDecimal totalCredits;
        private final BigDecimal balance;
        
        public AccountSummary(BigDecimal totalDebits, BigDecimal totalCredits, BigDecimal balance) {
            this.totalDebits = totalDebits;
            this.totalCredits = totalCredits;
            this.balance = balance;
        }
        
        public BigDecimal getTotalDebits() { return totalDebits; }
        public BigDecimal getTotalCredits() { return totalCredits; }
        public BigDecimal getBalance() { return balance; }
    }
}
