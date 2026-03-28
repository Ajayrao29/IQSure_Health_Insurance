// Data Transfer Object for DiscountRuleResponseDTO

package org.hartford.iqsure.dto.response;
import lombok.*;
import org.hartford.iqsure.entity.Policy;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRuleResponseDTO {
    private Long ruleId;
    private String ruleName;
    private String description;
    private Double minQuizScorePercent;
    private Integer minUserPoints;
    private Integer minBadgesEarned;
    private Double discountPercentage;
    private Policy.PolicyType applicablePolicyType;
    private Boolean isActive;
}