package com.example.tokenservice.unit;

import com.example.tokenservice.fraud.entity.FraudCheck;
import com.example.tokenservice.fraud.entity.FraudRule;
import com.example.tokenservice.fraud.repository.FraudCheckRepository;
import com.example.tokenservice.fraud.repository.FraudRuleRepository;
import com.example.tokenservice.fraud.service.FraudService;
import com.example.tokenservice.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
public class FraudServiceTest {
    
    @Mock
    private FraudRuleRepository fraudRuleRepository;
    
    @Mock
    private FraudCheckRepository fraudCheckRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @InjectMocks
    private FraudService fraudService;
    
    private UUID accountId;
    private BigDecimal amount;
    private String transactionId;
    private FraudRule amountThresholdRule;
    private FraudRule frequencyRule;
    private FraudRule patternRule;
    
    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        amount = new BigDecimal("1000.00");
        transactionId = "txn_123";
        
        amountThresholdRule = FraudRule.builder()
                .id(1L)
                .ruleName("AMOUNT_THRESHOLD")
                .ruleType("AMOUNT_THRESHOLD")
                .condition("max_amount:500.00")
                .action("BLOCK")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        frequencyRule = FraudRule.builder()
                .id(2L)
                .ruleName("FREQUENCY_LIMIT")
                .ruleType("FREQUENCY")
                .condition("max_daily_transactions:5")
                .action("FLAG")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        patternRule = FraudRule.builder()
                .id(3L)
                .ruleName("SUSPICIOUS_PATTERN")
                .ruleType("PATTERN")
                .condition("rapid_transactions")
                .action("BLOCK")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testCheckFraudAllRulesPass() {
        List<FraudRule> activeRules = List.of(amountThresholdRule, frequencyRule, patternRule);
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(activeRules);
        when(transactionRepository.countSuccessfulTransactionsByAccount(accountId, LocalDateTime.now().minusHours(24)))
                .thenReturn(2L);
        when(fraudCheckRepository.findRecentChecksByAccount(accountId.toString(), LocalDateTime.now().minusMinutes(5)))
                .thenReturn(List.of());
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, new BigDecimal("100.00"), transactionId);
        
        assertTrue(result);
        verify(fraudRuleRepository).findByActiveTrue();
        verify(transactionRepository).countSuccessfulTransactionsByAccount(accountId, LocalDateTime.now().minusHours(24));
        verify(fraudCheckRepository).findRecentChecksByAccount(accountId.toString(), LocalDateTime.now().minusMinutes(5));
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
    
    @Test
    void testCheckFraudAmountThresholdBlocked() {
        List<FraudRule> activeRules = List.of(amountThresholdRule);
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(activeRules);
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, new BigDecimal("1000.00"), transactionId);
        
        assertFalse(result);
        verify(fraudRuleRepository).findByActiveTrue();
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
    
    @Test
    void testCheckFraudFrequencyFlagged() {
        List<FraudRule> activeRules = List.of(frequencyRule);
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(activeRules);
        when(transactionRepository.countSuccessfulTransactionsByAccount(accountId, LocalDateTime.now().minusHours(24)))
                .thenReturn(6L);
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, new BigDecimal("100.00"), transactionId);
        
        assertFalse(result); // Should be blocked because FLAG action leads to failed status
        verify(fraudRuleRepository).findByActiveTrue();
        verify(transactionRepository).countSuccessfulTransactionsByAccount(accountId, LocalDateTime.now().minusHours(24));
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
    
    @Test
    void testCheckFraudPatternBlocked() {
        List<FraudRule> activeRules = List.of(patternRule);
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(activeRules);
        
        FraudCheck recentCheck = FraudCheck.builder()
                .checkId("fc_1")
                .transactionId("txn_1")
                .accountId(accountId)
                .amount(new BigDecimal("100.00"))
                .result("PASSED")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();
        
        FraudCheck recentCheck2 = FraudCheck.builder()
                .checkId("fc_2")
                .transactionId("txn_2")
                .accountId(accountId)
                .amount(new BigDecimal("100.00"))
                .result("PASSED")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();
        
        FraudCheck recentCheck3 = FraudCheck.builder()
                .checkId("fc_3")
                .transactionId("txn_3")
                .accountId(accountId)
                .amount(new BigDecimal("100.00"))
                .result("PASSED")
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .build();
        
        when(fraudCheckRepository.findRecentChecksByAccount(accountId.toString(), LocalDateTime.now().minusMinutes(5)))
                .thenReturn(List.of(recentCheck, recentCheck2, recentCheck3));
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, new BigDecimal("100.00"), transactionId);
        
        assertFalse(result);
        verify(fraudRuleRepository).findByActiveTrue();
        verify(fraudCheckRepository).findRecentChecksByAccount(accountId.toString(), LocalDateTime.now().minusMinutes(5));
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
    
    @Test
    void testCheckFraudSuspiciousAmountPattern() {
        List<FraudRule> activeRules = List.of(patternRule);
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(activeRules);
        when(fraudCheckRepository.findRecentChecksByAccount(accountId.toString(), LocalDateTime.now().minusMinutes(5)))
                .thenReturn(List.of());
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, new BigDecimal("999.99"), transactionId);
        
        assertFalse(result);
        verify(fraudRuleRepository).findByActiveTrue();
        verify(fraudCheckRepository).findRecentChecksByAccount(accountId.toString(), LocalDateTime.now().minusMinutes(5));
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
    
    @Test
    void testCheckFraudNoActiveRules() {
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(List.of());
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, amount, transactionId);
        
        assertTrue(result);
        verify(fraudRuleRepository).findByActiveTrue();
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
    
    @Test
    void testGetAccountFraudChecks() {
        List<FraudCheck> expectedChecks = List.of(
                FraudCheck.builder()
                        .checkId("fc_1")
                        .transactionId("txn_1")
                        .accountId(accountId)
                        .amount(amount)
                        .result("PASSED")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        
        when(fraudCheckRepository.findByAccountId(accountId)).thenReturn(expectedChecks);
        
        List<FraudCheck> result = fraudService.getAccountFraudChecks(accountId);
        
        assertEquals(expectedChecks, result);
        verify(fraudCheckRepository).findByAccountId(accountId);
    }
    
    @Test
    void testGetActiveRules() {
        List<FraudRule> expectedRules = List.of(amountThresholdRule, frequencyRule, patternRule);
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(expectedRules);
        
        List<FraudRule> result = fraudService.getActiveRules();
        
        assertEquals(expectedRules, result);
        verify(fraudRuleRepository).findByActiveTrue();
    }
    
    @Test
    void testCreateRule() {
        String ruleName = "NEW_RULE";
        String ruleType = "AMOUNT_THRESHOLD";
        String condition = "max_amount:2000.00";
        String action = "FLAG";
        
        FraudRule expectedRule = FraudRule.builder()
                .ruleName(ruleName)
                .ruleType(ruleType)
                .condition(condition)
                .action(action)
                .active(true)
                .build();
        
        when(fraudRuleRepository.save(any(FraudRule.class))).thenReturn(expectedRule);
        
        FraudRule result = fraudService.createRule(ruleName, ruleType, condition, action);
        
        assertEquals(ruleName, result.getRuleName());
        assertEquals(ruleType, result.getRuleType());
        assertEquals(condition, result.getCondition());
        assertEquals(action, result.getAction());
        assertTrue(result.getActive());
        verify(fraudRuleRepository).save(any(FraudRule.class));
    }
    
    @Test
    void testEvaluateAmountThresholdRuleNotTriggered() {
        FraudRule rule = FraudRule.builder()
                .ruleName("AMOUNT_THRESHOLD")
                .ruleType("AMOUNT_THRESHOLD")
                .condition("max_amount:500.00")
                .action("BLOCK")
                .build();
        
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, new BigDecimal("300.00"), transactionId);
        
        assertTrue(result);
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
    
    @Test
    void testEvaluateFrequencyRuleNotTriggered() {
        FraudRule rule = FraudRule.builder()
                .ruleName("FREQUENCY_LIMIT")
                .ruleType("FREQUENCY")
                .condition("max_daily_transactions:10")
                .action("FLAG")
                .build();
        
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(transactionRepository.countSuccessfulTransactionsByAccount(accountId, LocalDateTime.now().minusHours(24)))
                .thenReturn(3L);
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, new BigDecimal("100.00"), transactionId);
        
        assertTrue(result);
        verify(transactionRepository).countSuccessfulTransactionsByAccount(accountId, LocalDateTime.now().minusHours(24));
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
    
    @Test
    void testEvaluatePatternRuleNotTriggered() {
        FraudRule rule = FraudRule.builder()
                .ruleName("SUSPICIOUS_PATTERN")
                .ruleType("PATTERN")
                .condition("rapid_transactions")
                .action("BLOCK")
                .build();
        
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(fraudCheckRepository.findRecentChecksByAccount(accountId.toString(), LocalDateTime.now().minusMinutes(5)))
                .thenReturn(List.of());
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, new BigDecimal("100.00"), transactionId);
        
        assertTrue(result);
        verify(fraudCheckRepository).findRecentChecksByAccount(accountId.toString(), LocalDateTime.now().minusMinutes(5));
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
    
    @Test
    void testEvaluateUnknownRuleType() {
        FraudRule rule = FraudRule.builder()
                .ruleName("UNKNOWN_RULE")
                .ruleType("UNKNOWN")
                .condition("some_condition")
                .action("BLOCK")
                .build();
        
        when(fraudRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = fraudService.checkFraud(accountId, amount, transactionId);
        
        assertTrue(result);
        verify(fraudCheckRepository).save(any(FraudCheck.class));
    }
}
