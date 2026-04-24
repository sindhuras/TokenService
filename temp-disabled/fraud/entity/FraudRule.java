package com.example.tokenservice.fraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fraud_rules")
public class FraudRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rule_name", unique = true, nullable = false)
    private String ruleName;
    
    @Column(nullable = false)
    private String ruleType; // AMOUNT_THRESHOLD, FREQUENCY, PATTERN
    
    @Column(nullable = false)
    private String condition; // JSON condition
    
    @Column(nullable = false)
    private String action; // BLOCK, FLAG, ALLOW
    
    @Column(nullable = false)
    private Boolean active;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
