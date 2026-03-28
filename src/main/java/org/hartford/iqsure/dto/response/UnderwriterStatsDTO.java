package org.hartford.iqsure.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for sending underwriter performance statistics to the dashboard.
 */
@Data
@Builder
public class UnderwriterStatsDTO {
    private long pendingAssignments;
    private long quotesSent;
    private long activePolicies;
    private long customersServed;
    private double totalPremium;
    private double commissionEarned;
}
