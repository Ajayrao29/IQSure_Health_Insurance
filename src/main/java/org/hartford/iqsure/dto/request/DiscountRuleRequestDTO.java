// Data Transfer Object for DiscountRuleRequestDTO

package org.hartford.iqsure.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hartford.iqsure.entity.Policy;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRuleRequestDTO {
    @NotBlank(message = "Rule name is required")
    private String ruleName;
    private String description;
    @DecimalMin(value = "0.0", message = "Min quiz score cannot be negative")
    @DecimalMax(value = "100.0", message = "Min quiz score cannot exceed 100")
    @Builder.Default
    private Double minQuizScorePercent = 0.0;
    @Min(value = 0, message = "Min user points cannot be negative")
    @Builder.Default
    private Integer minUserPoints = 0;
    @Min(value = 0, message = "Min badges earned cannot be negative")
    @Builder.Default
    private Integer minBadgesEarned = 0;
    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.1", message = "Discount must be at least 0.1%")
    @DecimalMax(value = "50.0", message = "Single rule discount cannot exceed 50%")
    private Double discountPercentage;
    private Policy.PolicyType applicablePolicyType;
    @Builder.Default
    private Boolean isActive = true;
}