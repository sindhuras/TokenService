package com.example.tokenservice.transaction.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;
    
    @Column(name = "from_account", nullable = false)
    private UUID fromAccount;
    
    @Column(name = "to_account", nullable = false)
    private UUID toAccount;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(nullable = false)
    private String status; // PENDING, SUCCESS, FAILED, CANCELLED
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @Column(name = "payment_token_id")
    private String paymentTokenId;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "fraud_check_result")
    private String fraudCheckResult;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Transaction() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public UUID getFromAccount() { return fromAccount; }
    public void setFromAccount(UUID fromAccount) { this.fromAccount = fromAccount; }
    
    public UUID getToAccount() { return toAccount; }
    public void setToAccount(UUID toAccount) { this.toAccount = toAccount; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    
    public String getPaymentTokenId() { return paymentTokenId; }
    public void setPaymentTokenId(String paymentTokenId) { this.paymentTokenId = paymentTokenId; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public String getFraudCheckResult() { return fraudCheckResult; }
    public void setFraudCheckResult(String fraudCheckResult) { this.fraudCheckResult = fraudCheckResult; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Static builder method
    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }
    
    public static class TransactionBuilder {
        private Transaction transaction = new Transaction();
        
        public TransactionBuilder transactionId(String transactionId) {
            transaction.setTransactionId(transactionId);
            return this;
        }
        
        public TransactionBuilder fromAccount(UUID fromAccount) {
            transaction.setFromAccount(fromAccount);
            return this;
        }
        
        public TransactionBuilder toAccount(UUID toAccount) {
            transaction.setToAccount(toAccount);
            return this;
        }
        
        public TransactionBuilder amount(BigDecimal amount) {
            transaction.setAmount(amount);
            return this;
        }
        
        public TransactionBuilder currency(String currency) {
            transaction.setCurrency(currency);
            return this;
        }
        
        public TransactionBuilder status(String status) {
            transaction.setStatus(status);
            return this;
        }
        
        public TransactionBuilder idempotencyKey(String idempotencyKey) {
            transaction.setIdempotencyKey(idempotencyKey);
            return this;
        }
        
        public TransactionBuilder paymentTokenId(String paymentTokenId) {
            transaction.setPaymentTokenId(paymentTokenId);
            return this;
        }
        
        public Transaction build() {
            return transaction;
        }
    }
}
