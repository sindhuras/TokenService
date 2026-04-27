package com.example.tokenservice.transaction.repository;

import com.example.tokenservice.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByTransactionId(String transactionId);
    
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    List<Transaction> findByFromAccount(UUID fromAccount);
    
    List<Transaction> findByToAccount(UUID toAccount);
    
    List<Transaction> findByFromAccountAndStatus(UUID fromAccount, String status);
    
    List<Transaction> findByToAccountAndStatus(UUID toAccount, String status);
    
    @Query("SELECT t FROM Transaction t WHERE (t.fromAccount = :accountId OR t.toAccount = :accountId) AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findTransactionsByAccountAndDateRange(UUID accountId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.fromAccount = :accountId AND t.status = 'SUCCESS' AND t.createdAt >= :since")
    Long countSuccessfulTransactionsByAccount(UUID accountId, LocalDateTime since);
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.fromAccount = :accountId AND t.status = 'SUCCESS' AND t.createdAt >= :since")
    java.math.BigDecimal sumSuccessfulTransactionsByAccount(UUID accountId, LocalDateTime since);
}
