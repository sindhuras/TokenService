package com.example.tokenservice.ledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "entry_id", unique = true, nullable = false)
    private String entryId;
    
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;
    
    @Column(name = "account_id", nullable = false)
    private String accountId;
    
    @Column(nullable = false)
    private String type; // DEBIT, CREDIT
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(nullable = false)
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "balance_after")
    private BigDecimal balanceAfter;
    
    // Constructors
    public LedgerEntry() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    
    // Static builder method
    public static LedgerEntryBuilder builder() {
        return new LedgerEntryBuilder();
    }
    
    public static class LedgerEntryBuilder {
        private LedgerEntry ledgerEntry = new LedgerEntry();
        
        public LedgerEntryBuilder entryId(String entryId) {
            ledgerEntry.setEntryId(entryId);
            return this;
        }
        
        public LedgerEntryBuilder transactionId(String transactionId) {
            ledgerEntry.setTransactionId(transactionId);
            return this;
        }
        
        public LedgerEntryBuilder accountId(String accountId) {
            ledgerEntry.setAccountId(accountId);
            return this;
        }
        
        public LedgerEntryBuilder type(String type) {
            ledgerEntry.setType(type);
            return this;
        }
        
        public LedgerEntryBuilder amount(BigDecimal amount) {
            ledgerEntry.setAmount(amount);
            return this;
        }
        
        public LedgerEntryBuilder currency(String currency) {
            ledgerEntry.setCurrency(currency);
            return this;
        }
        
        public LedgerEntryBuilder description(String description) {
            ledgerEntry.setDescription(description);
            return this;
        }
        
        public LedgerEntryBuilder balanceAfter(BigDecimal balanceAfter) {
            ledgerEntry.setBalanceAfter(balanceAfter);
            return this;
        }
        
        public LedgerEntry build() {
            return ledgerEntry;
        }
    }
}
