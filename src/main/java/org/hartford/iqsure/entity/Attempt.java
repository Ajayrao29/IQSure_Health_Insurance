// Entity class representing Attempt in the database
package org.hartford.iqsure.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    private String quizTitle;
    private Integer score;
    private Integer totalQuestions;
    private Integer percentage;
    private Integer pointsEarned;
    @Builder.Default
    private LocalDateTime attemptDate = LocalDateTime.now();
}