package org.hartford.iqsure.dto.request;
import lombok.Data;
@Data
public class LessonCompletionRequest {
    private Long userId;
    private String topic;
    private int score;
    private int total;
    private String reportJson;
}
