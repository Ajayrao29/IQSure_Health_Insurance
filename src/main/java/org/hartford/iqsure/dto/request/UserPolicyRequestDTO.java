// Data Transfer Object for UserPolicyRequestDTO

package org.hartford.iqsure.dto.request;
import jakarta.validation.constraints.NotNull;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPolicyRequestDTO {
    @NotNull(message = "Policy ID is required")
    private Long policyId;
    private String nomineeName;
    private String nomineeRelationship;
    private String healthReportPath;
    private java.util.List<InsuredMemberRequestDTO> insuredMembers;
    private java.util.List<Long> rewardIds;
}