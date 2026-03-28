// Data Transfer Object for BadgeRequestDTO

package org.hartford.iqsure.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class BadgeRequestDTO {
    @NotBlank(message = "Badge name is required")
    private String name;
    private String description;
    @NotNull(message = "Required points is required")
    @Min(value = 0, message = "Required points must be 0 or more")
    private Integer reqPoints;
}