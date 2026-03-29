// Data Transfer Object for AttemptResponseDTO
package org.hartford.iqsure.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@Builder
public class AttemptResponseDTO {
    private Long attemptId;
    private Long userId;
    private Long quizId;
    private String quizTitle;
    private Integer score;
    private Integer totalQuestions;
    private Integer percentage;
    private Integer pointsEarned;
    private LocalDateTime attemptDate;
    private String questionReportJson;
}