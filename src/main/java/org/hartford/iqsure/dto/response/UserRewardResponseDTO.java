// Data Transfer Object for UserRewardResponseDTO
package org.hartford.iqsure.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@Builder
public class UserRewardResponseDTO {
    private Long userRewardId;
    private String rewardTitle;
    private String rewardType;
    private Double discountValue;
    private LocalDateTime expiryDate;
    private LocalDateTime earnedOn;
    private boolean used;
    private boolean isExpired;
}