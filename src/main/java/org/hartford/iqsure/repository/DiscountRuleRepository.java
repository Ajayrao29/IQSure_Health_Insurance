// Repository for database operations on DiscountRuleRepository

package org.hartford.iqsure.repository;
import org.hartford.iqsure.entity.DiscountRule;
import org.hartford.iqsure.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Long> {
    List<DiscountRule> findByIsActiveTrue();
    @Query("SELECT dr FROM DiscountRule dr WHERE dr.isActive = true " +
           "AND (dr.applicablePolicyType IS NULL OR dr.applicablePolicyType = :policyType)")
    List<DiscountRule> findActiveRulesForPolicyType(@Param("policyType") Policy.PolicyType policyType);
    boolean existsByRuleName(String ruleName);
}