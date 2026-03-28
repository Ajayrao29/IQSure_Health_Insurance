// Data Transfer Object for PremiumCalculationLogResponseDTO

package org.hartford.iqsure.dto.response;
import lombok.*;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumCalculationLogResponseDTO {
    private Long logId;
    private Long userId;
    private Long policyId;
    private String policyTitle;
    private Double basePremium;
    private Double totalDiscountPercent;
    private Double finalPremium;
    private Integer userPointsSnapshot;
    private Integer badgeCountSnapshot;
    private Double bestQuizScoreSnapshot;
    private String appliedRuleNames;
    private LocalDateTime calculatedAt;
}