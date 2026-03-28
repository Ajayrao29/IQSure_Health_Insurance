// Entity class representing PremiumCalculationLog in the database

package org.hartford.iqsure.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "premium_calculation_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumCalculationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;
    @Column(nullable = false)
    private Double basePremium;
    @Column(nullable = false)
    private Double totalDiscountPercent;
    @Column(nullable = false)
    private Double finalPremium;
    @Column(nullable = false)
    private Integer userPointsSnapshot;
    @Column(nullable = false)
    private Integer badgeCountSnapshot;
    @Column(nullable = false)
    private Double bestQuizScoreSnapshot;
    @Column(columnDefinition = "TEXT")
    private String appliedRuleNames;
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();
}