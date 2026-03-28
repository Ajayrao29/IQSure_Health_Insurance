// Entity class representing Policy in the database

package org.hartford.iqsure.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType policyType;
    @Column(nullable = false)
    private Double basePremium;
    @Column(nullable = false)
    private Double coverageAmount;
    @Column(nullable = false)
    private Integer durationMonths;
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
    private String ageRange;
    private String planType;
    private String waitingPeriod;
    private Boolean hasMaternityCover;
    private Boolean hasPreExistingCover;
    private Double deductibleAmount;
    private Double outOfPocketMax;
    private Double copayPercentage;
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserPolicy> userPolicies = new ArrayList<>();
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PremiumCalculationLog> premiumLogs = new ArrayList<>();
    public enum PolicyType {
        LIFE,
        HEALTH,
        MOTOR
    }
}