// Entity class representing User in the database

package org.hartford.iqsure.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hartford.iqsure.enums.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;
    private String phone;
    @Builder.Default
    private Integer userPoints = 0;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;
    private String licenseNumber;
    private String specialization;
    private BigDecimal commissionPercentage;
    @Builder.Default
    private Integer totalQuotesSent = 0;
    private String employeeId;
    private String department;
    @Builder.Default
    private BigDecimal approvalLimit = new BigDecimal("500000.00");
    @Builder.Default
    private Integer totalClaimsProcessed = 0;
    @Builder.Default
    private Integer totalClaimsApproved = 0;
    @Builder.Default
    private Integer totalClaimsRejected = 0;
    private String city;
    private String state;
    private String zipCode;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    @Builder.Default
    private Integer totalQuizzesTaken = 0;
    @Builder.Default
    private Integer currentStreak = 0;
    private LocalDate lastQuizDate;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserBadge> userBadges = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserReward> userRewards = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserPolicy> userPolicies = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PremiumCalculationLog> premiumLogs = new ArrayList<>();
    public enum Role {
        ROLE_USER,
        ROLE_ADMIN,
        ROLE_UNDERWRITER,
        ROLE_CLAIMS_OFFICER
    }
}