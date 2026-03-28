// Data Transfer Object for PremiumBreakdownDTO

package org.hartford.iqsure.dto.response;
import lombok.*;
import org.hartford.iqsure.entity.Policy;
import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumBreakdownDTO {
    private Long policyId;
    private String policyTitle;
    private Policy.PolicyType policyType;
    private Double basePremium;
    private Integer durationMonths;
    private Double coverageAmount;
    private Long userId;
    private Integer userPoints;
    private Integer badgesEarned;
    private Double bestQuizScorePercent;
    private List<AppliedDiscountDTO> appliedDiscounts;
    private Double totalDiscountPercent;
    private Double discountedAmount;
    private Double finalPremium;
    private LocalDateTime calculatedAt;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppliedDiscountDTO {
        private String ruleName;
        private Double discountPercentage;
        private String reason;
    }
}