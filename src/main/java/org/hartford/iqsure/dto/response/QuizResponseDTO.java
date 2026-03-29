package org.hartford.iqsure.dto.response;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponseDTO {
    private Long quizId;
    private String title;
    private String category;
    private String difficulty;
    private Integer totalQuestions;
}
