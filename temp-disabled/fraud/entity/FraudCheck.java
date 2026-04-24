package com.example.tokenservice.fraud.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_checks")
public class FraudCheck {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "check_id", unique = true, nullable = false)
    private String checkId;
    
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;
    
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String result; // PASSED, FAILED, FLAGGED
    
    @Column(name = "risk_score")
    private Integer riskScore;
    
    @Column(name = "triggered_rules")
    private String triggeredRules;
    
    @Column(name = "reason")
    private String reason;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public FraudCheck() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCheckId() { return checkId; }
    public void setCheckId(String checkId) { this.checkId = checkId; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    
    public String getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(String triggeredRules) { this.triggeredRules = triggeredRules; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Static builder method
    public static FraudCheckBuilder builder() {
        return new FraudCheckBuilder();
    }
    
    public static class FraudCheckBuilder {
        private FraudCheck fraudCheck = new FraudCheck();
        
        public FraudCheckBuilder checkId(String checkId) {
            fraudCheck.setCheckId(checkId);
            return this;
        }
        
        public FraudCheckBuilder transactionId(String transactionId) {
            fraudCheck.setTransactionId(transactionId);
            return this;
        }
        
        public FraudCheckBuilder accountId(UUID accountId) {
            fraudCheck.setAccountId(accountId);
            return this;
        }
        
        public FraudCheckBuilder amount(BigDecimal amount) {
            fraudCheck.setAmount(amount);
            return this;
        }
        
        public FraudCheckBuilder result(String result) {
            fraudCheck.setResult(result);
            return this;
        }
        
        public FraudCheckBuilder riskScore(Integer riskScore) {
            fraudCheck.setRiskScore(riskScore);
            return this;
        }
        
        public FraudCheck build() {
            return fraudCheck;
        }
    }
}
