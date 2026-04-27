package com.example.tokenservice.fraud.service;

import com.example.tokenservice.fraud.entity.FraudCheck;
import com.example.tokenservice.fraud.entity.FraudRule;
import com.example.tokenservice.fraud.repository.FraudCheckRepository;
import com.example.tokenservice.fraud.repository.FraudRuleRepository;
import com.example.tokenservice.transaction.repository.TransactionRepository;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.common.util.EncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FraudService {
    
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudCheckRepository fraudCheckRepository;
    private final TransactionRepository transactionRepository;
    
    // Constructor
    public FraudService(FraudRuleRepository fraudRuleRepository, FraudCheckRepository fraudCheckRepository, TransactionRepository transactionRepository) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudCheckRepository = fraudCheckRepository;
        this.transactionRepository = transactionRepository;
    }
    
    @Transactional
    public boolean checkFraud(UUID accountId, BigDecimal amount, String transactionId) {
        System.out.println("Performing fraud check for account: " + accountId + " amount: " + amount + " transaction: " + transactionId);
        
        List<FraudRule> activeRules = fraudRuleRepository.findByActiveTrue();
        
        FraudCheck.FraudCheckBuilder fraudCheckBuilder = FraudCheck.builder()
                .checkId(generateCheckId())
                .transactionId(transactionId)
                .accountId(accountId)
                .amount(amount)
                .result("PASSED")
                .riskScore(0);
        
        boolean blocked = false;
        int totalRiskScore = 0;
        StringBuilder triggeredRules = new StringBuilder();
        StringBuilder reasons = new StringBuilder();
        
        for (FraudRule rule : activeRules) {
            FraudRuleResult result = evaluateRule(rule, accountId, amount);
            
            if (result.isTriggered()) {
                totalRiskScore += result.getRiskScore();
                triggeredRules.append(rule.getRuleName()).append(",");
                reasons.append(result.getReason()).append("; ");
                
                if ("BLOCK".equals(rule.getAction())) {
                    blocked = true;
                    fraudCheckBuilder.result("FAILED");
                } else if ("FLAG".equals(rule.getAction()) && !"FAILED".equals(fraudCheckBuilder.build().getResult())) {
                    fraudCheckBuilder.result("FLAGGED");
                }
            }
        }
        
        // Create fraud check record
        FraudCheck fraudCheck = fraudCheckBuilder
                .riskScore(totalRiskScore)
                .triggeredRules(triggeredRules.length() > 0 ? triggeredRules.substring(0, triggeredRules.length() - 1) : null)
                .reason(reasons.length() > 0 ? reasons.substring(0, reasons.length() - 2) : null)
                .build();
        
        fraudCheckRepository.save(fraudCheck);
        
        log.info("Fraud check completed for transaction: {} result: {} riskScore: {}", 
                transactionId, fraudCheck.getResult(), totalRiskScore);
        
        return !blocked;
    }
    
    private FraudRuleResult evaluateRule(FraudRule rule, UUID accountId, BigDecimal amount) {
        switch (rule.getRuleType()) {
            case "AMOUNT_THRESHOLD":
                return evaluateAmountThreshold(rule, amount);
            case "FREQUENCY":
                return evaluateFrequency(rule, accountId);
            case "PATTERN":
                return evaluatePattern(rule, accountId, amount);
            default:
                return new FraudRuleResult(false, 0, "Unknown rule type");
        }
    }
    
    private FraudRuleResult evaluateAmountThreshold(FraudRule rule, BigDecimal amount) {
        // Simple amount threshold check
        if (rule.getCondition().contains("max_amount")) {
            String[] parts = rule.getCondition().split(":");
            BigDecimal maxAmount = new BigDecimal(parts[1].trim());
            
            if (amount.compareTo(maxAmount) > 0) {
                return new FraudRuleResult(true, 30, "Amount exceeds threshold of " + maxAmount);
            }
        }
        return new FraudRuleResult(false, 0, null);
    }
    
    private FraudRuleResult evaluateFrequency(FraudRule rule, UUID accountId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Long transactionCount = transactionRepository.countSuccessfulTransactionsByAccount(accountId, since);
        
        if (rule.getCondition().contains("max_daily_transactions")) {
            String[] parts = rule.getCondition().split(":");
            int maxTransactions = Integer.parseInt(parts[1].trim());
            
            if (transactionCount >= maxTransactions) {
                return new FraudRuleResult(true, 20, "Too many transactions in last 24 hours");
            }
        }
        
        return new FraudRuleResult(false, 0, null);
    }
    
    private FraudRuleResult evaluatePattern(FraudRule rule, UUID accountId, BigDecimal amount) {
        // Check for rapid successive transactions
        LocalDateTime since = LocalDateTime.now().minusMinutes(5);
        List<FraudCheck> recentChecks = fraudCheckRepository.findRecentChecksByAccount(accountId, since);
        
        if (recentChecks.size() >= 3) {
            return new FraudRuleResult(true, 25, "Rapid succession of transactions");
        }
        
        // Check for unusual amount patterns
        if (amount.compareTo(BigDecimal.valueOf(999.99)) == 0 || 
            amount.compareTo(BigDecimal.valueOf(9999.99)) == 0) {
            return new FraudRuleResult(true, 15, "Suspicious amount pattern");
        }
        
        return new FraudRuleResult(false, 0, null);
    }
    
    @Transactional(readOnly = true)
    public List<FraudCheck> getAccountFraudChecks(UUID accountId) {
        return fraudCheckRepository.findByAccountId(accountId);
    }
    
    @Transactional(readOnly = true)
    public List<FraudRule> getActiveRules() {
        return fraudRuleRepository.findByActiveTrue();
    }
    
    @Transactional
    public FraudRule createRule(String ruleName, String ruleType, String condition, String action) {
        FraudRule rule = FraudRule.builder()
                .ruleName(ruleName)
                .ruleType(ruleType)
                .condition(condition)
                .action(action)
                .active(true)
                .build();
        
        return fraudRuleRepository.save(rule);
    }
    
    private String generateCheckId() {
        return "fc_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private static class FraudRuleResult {
        private final boolean triggered;
        private final int riskScore;
        private final String reason;
        
        public FraudRuleResult(boolean triggered, int riskScore, String reason) {
            this.triggered = triggered;
            this.riskScore = riskScore;
            this.reason = reason;
        }
        
        public boolean isTriggered() { return triggered; }
        public int getRiskScore() { return riskScore; }
        public String getReason() { return reason; }
    }
}
