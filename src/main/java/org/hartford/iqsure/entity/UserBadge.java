// Entity class representing UserBadge in the database

package org.hartford.iqsure.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "user_badges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime earnedDate = LocalDateTime.now();
}