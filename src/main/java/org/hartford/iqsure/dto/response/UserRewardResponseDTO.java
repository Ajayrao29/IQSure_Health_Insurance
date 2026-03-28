package org.hartford.iqsure.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO for sending redeemed reward information back to the frontend.
 * This is used for showing coupons that can be applied to policy purchases.
 */
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
