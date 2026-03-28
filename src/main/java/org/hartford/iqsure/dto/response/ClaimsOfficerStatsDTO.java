package org.hartford.iqsure.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO for claims officer performance and queue statistics.
 */
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
