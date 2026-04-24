package com.example.tokenservice.fraud.controller;

import com.example.tokenservice.fraud.entity.FraudCheck;
import com.example.tokenservice.fraud.entity.FraudRule;
import com.example.tokenservice.fraud.dto.FraudCheckRequest;
import com.example.tokenservice.fraud.dto.FraudCheckResponse;
import com.example.tokenservice.fraud.service.FraudService;
import com.example.tokenservice.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {
    
    private final FraudService fraudService;
    
    @PostMapping("/check")
    public ResponseEntity<ApiResponse<FraudCheckResponse>> checkFraud(@Valid @RequestBody FraudCheckRequest request) {
        boolean allowed = fraudService.checkFraud(request.getAccountId(), request.getAmount(), request.getTransactionId());
        FraudCheckResponse response = new FraudCheckResponse(allowed);
        return ResponseEntity.ok(ApiResponse.success("Fraud check completed", response));
    }
    
    @GetMapping("/account/{accountId}/checks")
    public ResponseEntity<ApiResponse<List<FraudCheck>>> getAccountFraudChecks(@PathVariable UUID accountId) {
        List<FraudCheck> checks = fraudService.getAccountFraudChecks(accountId);
        return ResponseEntity.ok(ApiResponse.success(checks));
    }
    
    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<FraudRule>>> getActiveRules() {
        List<FraudRule> rules = fraudService.getActiveRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }
    
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<FraudRule>> createRule(@Valid @RequestBody CreateRuleRequest request) {
        FraudRule rule = fraudService.createRule(request.getRuleName(), request.getRuleType(), request.getCondition(), request.getAction());
        return ResponseEntity.ok(ApiResponse.success("Fraud rule created successfully", rule));
    }
    
    private static class CreateRuleRequest {
        private String ruleName;
        private String ruleType;
        private String condition;
        private String action;
        
        public String getRuleName() { return ruleName; }
        public String getRuleType() { return ruleType; }
        public String getCondition() { return condition; }
        public String getAction() { return action; }
        
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public void setRuleType(String ruleType) { this.ruleType = ruleType; }
        public void setCondition(String condition) { this.condition = condition; }
        public void setAction(String action) { this.action = action; }
    }
}
