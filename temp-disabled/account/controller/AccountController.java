package com.example.tokenservice.account.controller;

import com.example.tokenservice.account.dto.*;
import com.example.tokenservice.account.entity.Account;
import com.example.tokenservice.account.entity.BalanceLock;
import com.example.tokenservice.account.service.AccountService;
import com.example.tokenservice.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Account>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.getUserId(), request.getAccountType(), request.getCurrency());
        return ResponseEntity.ok(ApiResponse.success("Account created successfully", account));
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Account>> getAccount(@PathVariable UUID accountId) {
        Account account = accountService.getAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success(account));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Account>>> getUserAccounts(@PathVariable UUID userId) {
        List<Account> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }
    
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable UUID accountId) {
        BigDecimal balance = accountService.getBalance(accountId);
        BalanceResponse response = new BalanceResponse(accountId, balance);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<ApiResponse<Account>> deposit(@PathVariable UUID accountId, @Valid @RequestBody AmountRequest request) {
        Account account = accountService.deposit(accountId, request.getAmount());
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", account));
    }
    
    @PostMapping("/{accountId}/lock")
    public ResponseEntity<ApiResponse<BalanceLock>> lockAmount(@PathVariable UUID accountId, @Valid @RequestBody LockAmountRequest request) {
        BalanceLock balanceLock = accountService.lockAmount(accountId, request.getAmount(), request.getTransactionId());
        return ResponseEntity.ok(ApiResponse.success("Amount locked successfully", balanceLock));
    }
    
    @PostMapping("/lock/{lockId}/release")
    public ResponseEntity<ApiResponse<String>> releaseLock(@PathVariable String lockId) {
        accountService.releaseLock(lockId);
        return ResponseEntity.ok(ApiResponse.success("Lock released successfully"));
    }
    
    @PostMapping("/lock/{lockId}/consume")
    public ResponseEntity<ApiResponse<String>> consumeLock(@PathVariable String lockId) {
        accountService.consumeLock(lockId);
        return ResponseEntity.ok(ApiResponse.success("Lock consumed successfully"));
    }
}
