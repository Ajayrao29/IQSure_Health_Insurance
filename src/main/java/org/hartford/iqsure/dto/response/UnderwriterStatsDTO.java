// Data Transfer Object for UnderwriterStatsDTO
package org.hartford.iqsure.dto.response;
import lombok.Builder;
import lombok.Data;
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