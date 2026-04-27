package com.example.tokenservice.ledger.repository;

import com.example.tokenservice.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    
    List<LedgerEntry> findByTransactionId(String transactionId);
    
    List<LedgerEntry> findByAccountId(String accountId);
    
    List<LedgerEntry> findByAccountIdAndType(String accountId, String type);
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.accountId = :accountId AND le.createdAt BETWEEN :startDate AND :endDate ORDER BY le.createdAt DESC")
    List<LedgerEntry> findEntriesByAccountAndDateRange(String accountId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COALESCE(SUM(CASE WHEN le.type = 'DEBIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.accountId = :accountId")
    BigDecimal getTotalDebitsByAccount(String accountId);
    
    @Query("SELECT COALESCE(SUM(CASE WHEN le.type = 'CREDIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.accountId = :accountId")
    BigDecimal getTotalCreditsByAccount(String accountId);
    
    @Query("SELECT COALESCE(SUM(CASE WHEN le.type = 'CREDIT' THEN le.amount ELSE -le.amount END), 0) FROM LedgerEntry le WHERE le.accountId = :accountId")
    BigDecimal getBalanceByAccount(String accountId);
    
    @Query("SELECT COUNT(le) FROM LedgerEntry le WHERE le.transactionId = :transactionId")
    Long countEntriesByTransaction(String transactionId);
}
