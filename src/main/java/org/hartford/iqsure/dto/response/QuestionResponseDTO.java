// Data Transfer Object for QuestionResponseDTO

package org.hartford.iqsure.dto.response;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data
@Builder
public class QuestionResponseDTO {
    private Long questionId;
    private Long quizId;
    private String text;
    private List<String> options;
    private Integer correctOptionIndex;
}