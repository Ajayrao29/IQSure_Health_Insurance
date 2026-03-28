// Entity class representing UserReward in the database

package org.hartford.iqsure.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "user_rewards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id", nullable = false)
    private Reward reward;
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime redeemedDate = LocalDateTime.now();
    @Column(name = "is_used", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean used = false;
}