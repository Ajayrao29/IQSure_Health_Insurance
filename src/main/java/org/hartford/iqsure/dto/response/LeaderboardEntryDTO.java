// Data Transfer Object for LeaderboardEntryDTO

package org.hartford.iqsure.dto.response;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class LeaderboardEntryDTO {
    private Integer rank;
    private Long userId;
    private String name;
    private Integer userPoints;
    private long quizzesAttempted;
}