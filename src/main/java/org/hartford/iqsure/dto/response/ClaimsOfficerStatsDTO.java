// Data Transfer Object for ClaimsOfficerStatsDTO
package org.hartford.iqsure.dto.response;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data
@Builder
public class ClaimsOfficerStatsDTO {
    private long claimsInQueue;
    private long underReview;
    private long totalProcessed;
    private long approved;
    private long rejected;
    private String approvalRate;
    private String department;
    private BigDecimal approvalLimit;
}