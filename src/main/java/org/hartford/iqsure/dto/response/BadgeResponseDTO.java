// Data Transfer Object for BadgeResponseDTO

package org.hartford.iqsure.dto.response;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class BadgeResponseDTO {
    private Long badgeId;
    private String name;
    private String description;
    private Integer reqPoints;
}