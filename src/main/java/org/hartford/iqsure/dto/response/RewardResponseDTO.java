// Data Transfer Object for RewardResponseDTO

package org.hartford.iqsure.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
@Data
@Builder
public class RewardResponseDTO {
    private Long rewardId;
    private String rewardType;
    private Double discountValue;
    private LocalDate expiryDate;
}