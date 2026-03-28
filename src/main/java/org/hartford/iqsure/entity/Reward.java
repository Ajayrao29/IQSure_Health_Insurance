// Entity class representing Reward in the database

package org.hartford.iqsure.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "rewards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rewardId;
    @Column(nullable = false)
    private String rewardType;
    @Column(nullable = false)
    private Double discountValue;
    @Column
    private String description;
    @Column
    private Integer reqPoints;
    @Column(nullable = false)
    private LocalDate expiryDate;
    @OneToMany(mappedBy = "reward", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserReward> userRewards = new ArrayList<>();
}