package com.example.tokenservice.fraud.repository;

import com.example.tokenservice.fraud.entity.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {
    
    List<FraudRule> findByActiveTrue();
    
    List<FraudRule> findByRuleTypeAndActiveTrue(String ruleType);
}
