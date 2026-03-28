// Entity class representing UserPolicy in the database

package org.hartford.iqsure.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "user_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;
    @Column(nullable = false)
    private Double finalPremium;
    @Column(nullable = false)
    private Double discountApplied;
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime purchaseDate = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.PENDING_UNDERWRITING;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underwriter_id")
    private User assignedUnderwriter;
    private java.time.LocalDateTime assignedAt;
    private java.math.BigDecimal quoteAmount;
    @Column(columnDefinition = "TEXT")
    private String underwriterRemarks;
    private String nomineeName;
    private String nomineeRelationship;
    private String healthReportPath;
    @OneToMany(mappedBy = "userPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<InsuredMember> insuredMembers = new java.util.ArrayList<>();
    @Builder.Default
    private java.math.BigDecimal totalClaimedAmount = java.math.BigDecimal.ZERO;
    @Builder.Default
    private java.math.BigDecimal remainingCoverage = java.math.BigDecimal.ZERO;
    public enum PolicyStatus {
        PENDING_UNDERWRITING,
        UNDER_EVALUATION,
        QUOTES_SENT,
        ACTIVE,
        EXPIRED,
        CANCELLED,
        REJECTED
    }
}