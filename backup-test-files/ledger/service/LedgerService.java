package com.example.tokenservice.ledger.service;

import com.example.tokenservice.ledger.entity.LedgerEntry;
import com.example.tokenservice.ledger.repository.LedgerEntryRepository;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.common.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {
    
    private final LedgerEntryRepository ledgerEntryRepository;
    
    @Transactional
    public void createEntries(String transactionId, UUID fromAccountId, UUID toAccountId, BigDecimal amount) {
        log.info("Creating ledger entries for transaction: {} amount: {}", transactionId, amount);
        
        // Validate transaction has exactly 2 entries (double-entry principle)
        Long existingEntries = ledgerEntryRepository.countEntriesByTransaction(transactionId);
        if (existingEntries > 0) {
            throw new PaymentException("LEDGER_EXISTS", "Ledger entries already exist for this transaction");
        }
        
        // Get current balances
        BigDecimal fromAccountBalance = ledgerEntryRepository.getBalanceByAccount(fromAccountId.toString());
        BigDecimal toAccountBalance = ledgerEntryRepository.getBalanceByAccount(toAccountId.toString());
        
        // Create debit entry (from account)
        LedgerEntry debitEntry = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transactionId(transactionId)
                .accountId(fromAccountId.toString())
                .type("DEBIT")
                .amount(amount)
                .currency("USD")
                .description("Payment to account " + toAccountId.toString())
                .balanceAfter(fromAccountBalance.subtract(amount))
                .build();
        
        // Create credit entry (to account)
        LedgerEntry creditEntry = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transactionId(transactionId)
                .accountId(toAccountId.toString())
                .type("CREDIT")
                .amount(amount)
                .currency("USD")
                .description("Payment from account " + fromAccountId.toString())
                .balanceAfter(toAccountBalance.add(amount))
                .build();
        
        // Save both entries
        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);
        
        log.info("Ledger entries created successfully for transaction: {}", transactionId);
    }
    
    @Transactional(readOnly = true)
    public List<LedgerEntry> getTransactionEntries(String transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId);
    }
    
    @Transactional(readOnly = true)
    public List<LedgerEntry> getAccountEntries(UUID accountId, LocalDateTime startDate, LocalDateTime endDate) {
        return ledgerEntryRepository.findEntriesByAccountAndDateRange(accountId.toString(), startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(UUID accountId) {
        return ledgerEntryRepository.getBalanceByAccount(accountId.toString());
    }
    
    @Transactional(readOnly = true)
    public void validateDoubleEntry(String transactionId) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transactionId);
        
        if (entries.size() != 2) {
            throw new PaymentException("LEDGER_INVALID", "Transaction must have exactly 2 ledger entries");
        }
        
        BigDecimal totalDebits = entries.stream()
                .filter(entry -> "DEBIT".equals(entry.getType()))
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = entries.stream()
                .filter(entry -> "CREDIT".equals(entry.getType()))
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new PaymentException("LEDGER_IMBALANCE", "Debits and credits must be equal");
        }
        
        log.info("Double-entry validation passed for transaction: {}", transactionId);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalDebitsByAccount(UUID accountId) {
        return ledgerEntryRepository.getTotalDebitsByAccount(accountId.toString());
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalCreditsByAccount(UUID accountId) {
        return ledgerEntryRepository.getTotalCreditsByAccount(accountId.toString());
    }
    
    private String generateEntryId() {
        return "le_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
