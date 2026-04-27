package com.example.tokenservice.unit;

import com.example.tokenservice.ledger.entity.LedgerEntry;
import com.example.tokenservice.ledger.repository.LedgerEntryRepository;
import com.example.tokenservice.ledger.service.LedgerService;
import com.example.tokenservice.common.exception.PaymentException;
import com.example.tokenservice.common.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LedgerServiceTest {
    
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    
    @InjectMocks
    private LedgerService ledgerService;
    
    private String transactionId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private LedgerEntry debitEntry;
    private LedgerEntry creditEntry;
    
    @BeforeEach
    void setUp() {
        transactionId = "txn_123";
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();
        amount = new BigDecimal("100.00");
        
        debitEntry = LedgerEntry.builder()
                .id(1L)
                .entryId("le_debit_123")
                .transactionId(transactionId)
                .accountId(fromAccountId.toString())
                .type("DEBIT")
                .amount(amount)
                .currency("USD")
                .description("Payment to account " + toAccountId.toString())
                .balanceAfter(new BigDecimal("900.00"))
                .createdAt(LocalDateTime.now())
                .build();
        
        creditEntry = LedgerEntry.builder()
                .id(2L)
                .entryId("le_credit_123")
                .transactionId(transactionId)
                .accountId(toAccountId.toString())
                .type("CREDIT")
                .amount(amount)
                .currency("USD")
                .description("Payment from account " + fromAccountId.toString())
                .balanceAfter(new BigDecimal("1100.00"))
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testCreateEntriesSuccess() {
        when(ledgerEntryRepository.countEntriesByTransaction(transactionId)).thenReturn(0L);
        when(ledgerEntryRepository.getBalanceByAccount(fromAccountId.toString())).thenReturn(new BigDecimal("1000.00"));
        when(ledgerEntryRepository.getBalanceByAccount(toAccountId.toString())).thenReturn(new BigDecimal("1000.00"));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(debitEntry).thenReturn(creditEntry);
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            
            ledgerService.createEntries(transactionId, fromAccountId, toAccountId, amount);
            
            verify(ledgerEntryRepository).countEntriesByTransaction(transactionId);
            verify(ledgerEntryRepository, times(2)).getBalanceByAccount(anyString());
            verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
        }
    }
    
    @Test
    void testCreateEntriesLedgerExists() {
        when(ledgerEntryRepository.countEntriesByTransaction(transactionId)).thenReturn(2L);
        
        assertThrows(PaymentException.class, () -> ledgerService.createEntries(transactionId, fromAccountId, toAccountId, amount));
        
        verify(ledgerEntryRepository).countEntriesByTransaction(transactionId);
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }
    
    @Test
    void testGetTransactionEntries() {
        List<LedgerEntry> expectedEntries = List.of(debitEntry, creditEntry);
        when(ledgerEntryRepository.findByTransactionId(transactionId)).thenReturn(expectedEntries);
        
        List<LedgerEntry> result = ledgerService.getTransactionEntries(transactionId);
        
        assertEquals(expectedEntries, result);
        verify(ledgerEntryRepository).findByTransactionId(transactionId);
    }
    
    @Test
    void testGetAccountEntries() {
        UUID accountId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();
        List<LedgerEntry> expectedEntries = List.of(debitEntry);
        
        when(ledgerEntryRepository.findEntriesByAccountAndDateRange(accountId.toString(), startDate, endDate))
                .thenReturn(expectedEntries);
        
        List<LedgerEntry> result = ledgerService.getAccountEntries(accountId, startDate, endDate);
        
        assertEquals(expectedEntries, result);
        verify(ledgerEntryRepository).findEntriesByAccountAndDateRange(accountId.toString(), startDate, endDate);
    }
    
    @Test
    void testGetAccountBalance() {
        UUID accountId = UUID.randomUUID();
        BigDecimal expectedBalance = new BigDecimal("1000.00");
        
        when(ledgerEntryRepository.getBalanceByAccount(accountId.toString())).thenReturn(expectedBalance);
        
        BigDecimal result = ledgerService.getAccountBalance(accountId);
        
        assertEquals(expectedBalance, result);
        verify(ledgerEntryRepository).getBalanceByAccount(accountId.toString());
    }
    
    @Test
    void testValidateDoubleEntrySuccess() {
        List<LedgerEntry> entries = List.of(debitEntry, creditEntry);
        when(ledgerEntryRepository.findByTransactionId(transactionId)).thenReturn(entries);
        when(ledgerEntryRepository.getTotalDebitsByAccount(fromAccountId.toString())).thenReturn(amount);
        when(ledgerEntryRepository.getTotalCreditsByAccount(toAccountId.toString())).thenReturn(amount);
        
        ledgerService.validateDoubleEntry(transactionId);
        
        verify(ledgerEntryRepository).findByTransactionId(transactionId);
        verify(ledgerEntryRepository).getTotalDebitsByAccount(fromAccountId.toString());
        verify(ledgerEntryRepository).getTotalCreditsByAccount(toAccountId.toString());
    }
    
    @Test
    void testValidateDoubleEntryWrongCount() {
        List<LedgerEntry> entries = List.of(debitEntry); // Only one entry
        when(ledgerEntryRepository.findByTransactionId(transactionId)).thenReturn(entries);
        
        assertThrows(PaymentException.class, () -> ledgerService.validateDoubleEntry(transactionId));
        
        verify(ledgerEntryRepository).findByTransactionId(transactionId);
        verify(ledgerEntryRepository, never()).getTotalDebitsByAccount(anyString());
        verify(ledgerEntryRepository, never()).getTotalCreditsByAccount(anyString());
    }
    
    @Test
    void testValidateDoubleEntryImbalance() {
        List<LedgerEntry> entries = List.of(debitEntry, creditEntry);
        when(ledgerEntryRepository.findByTransactionId(transactionId)).thenReturn(entries);
        when(ledgerEntryRepository.getTotalDebitsByAccount(fromAccountId.toString())).thenReturn(new BigDecimal("100.00"));
        when(ledgerEntryRepository.getTotalCreditsByAccount(toAccountId.toString())).thenReturn(new BigDecimal("200.00"));
        
        assertThrows(PaymentException.class, () -> ledgerService.validateDoubleEntry(transactionId));
        
        verify(ledgerEntryRepository).findByTransactionId(transactionId);
        verify(ledgerEntryRepository).getTotalDebitsByAccount(fromAccountId.toString());
        verify(ledgerEntryRepository).getTotalCreditsByAccount(toAccountId.toString());
    }
    
    @Test
    void testGetTotalDebitsByAccount() {
        UUID accountId = UUID.randomUUID();
        BigDecimal expectedDebits = new BigDecimal("500.00");
        
        when(ledgerEntryRepository.getTotalDebitsByAccount(accountId.toString())).thenReturn(expectedDebits);
        
        BigDecimal result = ledgerService.getTotalDebitsByAccount(accountId);
        
        assertEquals(expectedDebits, result);
        verify(ledgerEntryRepository).getTotalDebitsByAccount(accountId.toString());
    }
    
    @Test
    void testGetTotalCreditsByAccount() {
        UUID accountId = UUID.randomUUID();
        BigDecimal expectedCredits = new BigDecimal("700.00");
        
        when(ledgerEntryRepository.getTotalCreditsByAccount(accountId.toString())).thenReturn(expectedCredits);
        
        BigDecimal result = ledgerService.getTotalCreditsByAccount(accountId);
        
        assertEquals(expectedCredits, result);
        verify(ledgerEntryRepository).getTotalCreditsByAccount(accountId.toString());
    }
    
    @Test
    void testCreateEntriesWithZeroBalance() {
        when(ledgerEntryRepository.countEntriesByTransaction(transactionId)).thenReturn(0L);
        when(ledgerEntryRepository.getBalanceByAccount(fromAccountId.toString())).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.getBalanceByAccount(toAccountId.toString())).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(debitEntry).thenReturn(creditEntry);
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            
            ledgerService.createEntries(transactionId, fromAccountId, toAccountId, amount);
            
            verify(ledgerEntryRepository).countEntriesByTransaction(transactionId);
            verify(ledgerEntryRepository, times(2)).getBalanceByAccount(anyString());
            verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
        }
    }
    
    @Test
    void testCreateEntriesWithNegativeBalance() {
        when(ledgerEntryRepository.countEntriesByTransaction(transactionId)).thenReturn(0L);
        when(ledgerEntryRepository.getBalanceByAccount(fromAccountId.toString())).thenReturn(new BigDecimal("-100.00"));
        when(ledgerEntryRepository.getBalanceByAccount(toAccountId.toString())).thenReturn(new BigDecimal("-100.00"));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(debitEntry).thenReturn(creditEntry);
        
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.generateToken()).thenReturn("generatedToken");
            
            ledgerService.createEntries(transactionId, fromAccountId, toAccountId, amount);
            
            verify(ledgerEntryRepository).countEntriesByTransaction(transactionId);
            verify(ledgerEntryRepository, times(2)).getBalanceByAccount(anyString());
            verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
        }
    }
    
    @Test
    void testValidateDoubleEntryWithMoreThanTwoEntries() {
        LedgerEntry extraEntry = LedgerEntry.builder()
                .id(3L)
                .entryId("le_extra_123")
                .transactionId(transactionId)
                .accountId(UUID.randomUUID().toString())
                .type("DEBIT")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .description("Extra entry")
                .createdAt(LocalDateTime.now())
                .build();
        
        List<LedgerEntry> entries = List.of(debitEntry, creditEntry, extraEntry);
        when(ledgerEntryRepository.findByTransactionId(transactionId)).thenReturn(entries);
        
        assertThrows(PaymentException.class, () -> ledgerService.validateDoubleEntry(transactionId));
        
        verify(ledgerEntryRepository).findByTransactionId(transactionId);
        verify(ledgerEntryRepository, never()).getTotalDebitsByAccount(anyString());
        verify(ledgerEntryRepository, never()).getTotalCreditsByAccount(anyString());
    }
}
