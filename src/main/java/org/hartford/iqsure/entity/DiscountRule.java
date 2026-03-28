// Entity class representing DiscountRule in the database

package org.hartford.iqsure.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "discount_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;
    @Column(nullable = false, unique = true)
    private String ruleName;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Builder.Default
    @Column(nullable = false)
    private Double minQuizScorePercent = 0.0;
    @Builder.Default
    @Column(nullable = false)
    private Integer minUserPoints = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer minBadgesEarned = 0;
    @Column(nullable = false)
    private Double discountPercentage;
    @Enumerated(EnumType.STRING)
    private Policy.PolicyType applicablePolicyType;
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
}