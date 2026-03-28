// Data Transfer Object for UserPolicyResponseDTO

package org.hartford.iqsure.dto.response;
import lombok.*;
import org.hartford.iqsure.entity.Policy;
import org.hartford.iqsure.entity.UserPolicy;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPolicyResponseDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Long policyId;
    private String policyTitle;
    private Policy.PolicyType policyType;
    private Double basePremium;
    private Double coverageAmount;
    private Integer durationMonths;
    private Double finalPremium;
    private Double discountApplied;
    private LocalDateTime purchaseDate;
    private UserPolicy.PolicyStatus status;
    private Double savedAmount;
    private Long assignedUnderwriterId;
    private String assignedUnderwriterName;
    private java.time.LocalDateTime assignedAt;
    private java.math.BigDecimal quoteAmount;
    private String underwriterRemarks;
    private java.math.BigDecimal totalClaimedAmount;
    private java.math.BigDecimal remainingCoverage;
    private String nomineeName;
    private String nomineeRelationship;
    private String healthReportPath;
    private java.util.List<InsuredMemberResponseDTO> insuredMembers;
}