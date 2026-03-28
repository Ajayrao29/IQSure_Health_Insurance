// Data Transfer Object for PolicyResponseDTO

package org.hartford.iqsure.dto.response;
import lombok.*;
import org.hartford.iqsure.entity.Policy;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyResponseDTO {
    private Long policyId;
    private String title;
    private String description;
    private Policy.PolicyType policyType;
    private Double basePremium;
    private Double coverageAmount;
    private Integer durationMonths;
    private Boolean isActive;
    private String ageRange;
    private String planType;
    private String waitingPeriod;
    private Boolean hasMaternityCover;
    private Boolean hasPreExistingCover;
    private Double deductibleAmount;
    private Double outOfPocketMax;
    private Double copayPercentage;
}