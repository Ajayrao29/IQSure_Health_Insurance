/*
 * ============================================================================
 * FILE: DataSeeder.java
 * LOCATION: src/main/java/org/hartford/iqsure/config/
 * PURPOSE: Automatically creates a default ADMIN user when the app starts
 *          for the FIRST TIME (when the database is empty).
 *          This ensures there's always an admin who can log in and set up
 *          quizzes, policies, badges, etc. through the admin panel.
 *
 * HOW IT WORKS:
 *   - @PostConstruct → Spring calls seedAdmin() after this bean is created
 *     and all dependencies (userRepository, passwordEncoder) are injected
 *   - Checks if any users exist in the database
 *   - If NO users exist → creates an admin with email "admin@iqsure.com"
 *   - If users already exist → does nothing (skips seeding)
 *
 * DEFAULT ADMIN CREDENTIALS:
 *   - Email:    admin@iqsure.com
 *   - Password: admin123
 *
 * ANNOTATIONS EXPLAINED:
 *   - @Slf4j (Lombok) → Auto-creates a "log" variable for printing messages
 *   - @Component → Tells Spring: "create an instance of this class automatically"
 *   - @RequiredArgsConstructor (Lombok) → Auto-creates constructor for 'final' fields
 *   - @PostConstruct → Method runs once after bean initialization (replaces CommandLineRunner)
 *
 * CONNECTS TO:
 *   - UserRepository.java (repository/) → to check user count and save admin
 *   - SecurityConfig.java (config/) → provides the PasswordEncoder for hashing
 *   - User.java (entity/) → the User entity that gets saved to the "users" table
 * ============================================================================
 */
package org.hartford.iqsure.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hartford.iqsure.entity.User;
import org.hartford.iqsure.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.hartford.iqsure.repository.PolicyRepository policyRepository;
    private final org.hartford.iqsure.repository.BadgeRepository badgeRepository;
    private final org.hartford.iqsure.repository.RewardRepository rewardRepository;
    private final org.hartford.iqsure.repository.DiscountRuleRepository discountRuleRepository;
    private final org.hartford.iqsure.repository.UserPolicyRepository userPolicyRepository;
    private final org.hartford.iqsure.repository.InsuredMemberRepository insuredMemberRepository;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    // This method runs automatically after the bean is created and dependencies are injected
    @PostConstruct
    public void seedAdmin() {
        // Seed Admin if not exists
        if (userRepository.findByEmail("admin@iqsure.com").isEmpty()) {
            userRepository.save(User.builder()
                    .name("Admin")
                    .email("admin@iqsure.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("1234567890")
                    .role(User.Role.ROLE_ADMIN)
                    .userPoints(0)
                    .build());
            log.info("Admin user created: admin@iqsure.com / admin123");
        }

        // Seed Underwriter if not exists
        if (userRepository.findByEmail("underwriter@iqsure.com").isEmpty()) {
            userRepository.save(User.builder()
                    .name("Underwriter1")
                    .email("underwriter@iqsure.com")
                    .password(passwordEncoder.encode("underwriter123"))
                    .role(User.Role.ROLE_UNDERWRITER)
                    .licenseNumber("UW-88990")
                    .specialization("HEALTH")
                    .commissionPercentage(new java.math.BigDecimal("5.5"))
                    .status("ACTIVE")
                    .build());
            log.info("Underwriter created: underwriter@iqsure.com / underwriter123");
        }

        // Seed Claims Officer if not exists
        if (userRepository.findByEmail("bob@iqsure.com").isEmpty()) {
            userRepository.save(User.builder()
                    .name("Claims1")
                    .email("bob@iqsure.com")
                    .password(passwordEncoder.encode("claims123"))
                    .role(User.Role.ROLE_CLAIMS_OFFICER)
                    .employeeId("EMP-1234")
                    .department("CLAIMS")
                    .approvalLimit(new java.math.BigDecimal("750000.00"))
                    .status("ACTIVE")
                    .build());
            log.info("Claims Officer created: bob@iqsure.com / claims123");
        }

        // Seed Default User if not exists
        if (userRepository.findByEmail("user@iqsure.com").isEmpty()) {
            userRepository.save(User.builder()
                    .name("John Doe")
                    .email("user@iqsure.com")
                    .password(passwordEncoder.encode("user123"))
                    .role(User.Role.ROLE_USER)
                    .phone("9876543210")
                    .userPoints(500)
                    .city("New York")
                    .state("NY")
                    .status("ACTIVE")
                    .build());
            userRepository.flush(); // Ensure user is in DB
            log.info("Default user created: user@iqsure.com / user123");
        }

        if (policyRepository.count() == 0) {
            seedPolicies();
        }

        if (badgeRepository.count() == 0) {
            seedBadges();
        }

        if (rewardRepository.count() == 0) {
            seedRewards();
        }

        if (discountRuleRepository.count() == 0) {
            seedDiscountRules();
        }

        // Ensure we have sample data for the assignment pipeline
        if (userPolicyRepository.count() == 0) {
            seedSampleUserPolicy();
        }
    }

    private void seedSampleUserPolicy() {
        log.info("Attempting to seed sample UserPolicy...");
        try {
            User user = userRepository.findByEmail("user@iqsure.com").orElse(null);
            org.hartford.iqsure.entity.Policy policy = policyRepository.findAll().stream()
                    .filter(p -> p.getTitle().equals("Basic Health Plan"))
                    .findFirst()
                    .orElse(policyRepository.findAll().stream().findFirst().orElse(null));

            if (user != null && policy != null) {
                User underwriter = userRepository.findByEmail("underwriter@iqsure.com").orElse(null);
                
                org.hartford.iqsure.entity.UserPolicy up = org.hartford.iqsure.entity.UserPolicy.builder()
                        .user(user)
                        .policy(policy)
                        .finalPremium(policy.getBasePremium())
                        .discountApplied(0.0)
                        .status(underwriter != null ? org.hartford.iqsure.entity.UserPolicy.PolicyStatus.UNDER_EVALUATION : org.hartford.iqsure.entity.UserPolicy.PolicyStatus.PENDING_UNDERWRITING)
                        .assignedUnderwriter(underwriter)
                        .assignedAt(underwriter != null ? java.time.LocalDateTime.now() : null)
                        .remainingCoverage(java.math.BigDecimal.valueOf(policy.getCoverageAmount()))
                        .nomineeName("Jane Doe")
                        .nomineeRelationship("Spouse")
                        .healthReportPath("/uploads/sample-report.pdf")
                        .purchaseDate(java.time.LocalDateTime.now())
                        .build();
                userPolicyRepository.saveAndFlush(up);

                // Add sample insured members for the underwriter to review
                org.hartford.iqsure.entity.InsuredMember family1 = org.hartford.iqsure.entity.InsuredMember.builder()
                        .userPolicy(up)
                        .fullName("John Doe")
                        .relationship("SELF")
                        .dateOfBirth(java.time.LocalDate.of(1990, 5, 15))
                        .gender("MALE")
                        .preExistingConditions("None")
                        .build();
                
                org.hartford.iqsure.entity.InsuredMember family2 = org.hartford.iqsure.entity.InsuredMember.builder()
                        .userPolicy(up)
                        .fullName("Jane Doe")
                        .relationship("SPOUSE")
                        .dateOfBirth(java.time.LocalDate.of(1992, 8, 20))
                        .gender("FEMALE")
                        .preExistingConditions("Asthma")
                        .build();

                insuredMemberRepository.save(family1);
                insuredMemberRepository.save(family2);
                
                // Add an additional ACTIVE policy so user can test Claims flow immediately
                org.hartford.iqsure.entity.Policy goldPolicy = policyRepository.findAll().stream()
                        .filter(p -> p.getTitle().equals("Gold Health Plan"))
                        .findFirst()
                        .orElse(null);

                if (goldPolicy != null) {
                    org.hartford.iqsure.entity.UserPolicy activeUp = org.hartford.iqsure.entity.UserPolicy.builder()
                            .user(user)
                            .policy(goldPolicy)
                            .finalPremium(goldPolicy.getBasePremium())
                            .discountApplied(0.0)
                            .status(org.hartford.iqsure.entity.UserPolicy.PolicyStatus.ACTIVE)
                            .remainingCoverage(java.math.BigDecimal.valueOf(goldPolicy.getCoverageAmount()))
                            .nomineeName("Jane Doe")
                            .nomineeRelationship("Spouse")
                            .purchaseDate(java.time.LocalDateTime.now().minusMonths(1))
                            .build();
                    userPolicyRepository.save(activeUp);
                }

                insuredMemberRepository.flush();
                userPolicyRepository.flush();

                log.info("SUCCESS: Seeded UNDER_EVALUATION policy for John Doe assigned to underwriter.");
            } else {
                if (user == null) log.warn("SEEDER ERROR: User 'user@iqsure.com' not found for policy seeding.");
                if (policy == null) log.warn("SEEDER ERROR: No Policies found for policy seeding.");
            }
        } catch (Exception e) {
            log.error("CRITICAL SEEDER ERROR: Failed to seed user policy: {}", e.getMessage());
        }
    }
    private void seedPolicies() {
        savePolicy("Basic Health Plan", "Essential health coverage for individuals with basic hospitalization, day-care procedures, and ambulance charges.", 5000.0, 300000.0, 12, "18-65", "INDIVIDUAL", "1 month", false, false);
        savePolicy("Silver Health Plan", "Enhanced coverage with higher sum insured, cashless treatment at 5000+ network hospitals, and pre/post hospitalization coverage.", 8500.0, 500000.0, 12, "18-65", "INDIVIDUAL", "2 months", false, false);
        savePolicy("Gold Health Plan", "Premium comprehensive coverage including maternity benefits, pre-existing disease cover after waiting period.", 15000.0, 1000000.0, 12, "18-65", "INDIVIDUAL", "3 months", true, true);
        savePolicy("Family Health Plan", "Comprehensive family floater plan covering spouse and up to 3 children. Includes maternity and restoration benefit.", 20000.0, 1500000.0, 12, "18-65", "FAMILY", "2 months", true, false);
        savePolicy("Senior Citizen Plan", "Tailored plan for senior citizens aged 60-80 years. Covers pre-existing diseases after 2-year waiting period.", 25000.0, 800000.0, 12, "60-80", "SENIOR_CITIZEN", "24 months", false, true);
        savePolicy("Platinum Health Plan", "Ultimate individual plan with maximum ₹20 Lakh coverage, air ambulance, and personal accident cover.", 30000.0, 2000000.0, 12, "18-55", "INDIVIDUAL", "1 month", true, true);
        savePolicy("Cyber Shield Protection", "Modern digital protection against identity theft, online fraud, and social media hacking. Includes 24/7 expert support.", 1200.0, 500000.0, 12, "13-75", "INDIVIDUAL", "Instant", false, false);

        policyRepository.flush();
        log.info("Health Insurance Policies seeded.");
    }

    private void savePolicy(String title, String desc, Double premium, Double coverage, Integer duration, String age, String type, String waiting, Boolean maternity, Boolean preExisting) {
        policyRepository.save(org.hartford.iqsure.entity.Policy.builder()
                .title(title)
                .description(desc)
                .basePremium(premium)
                .coverageAmount(coverage)
                .durationMonths(duration)
                .policyType(org.hartford.iqsure.entity.Policy.PolicyType.HEALTH)
                .ageRange(age)
                .planType(type)
                .waitingPeriod(waiting)
                .hasMaternityCover(maternity)
                .hasPreExistingCover(preExisting)
                .isActive(true)
                .build());
    }

    private void seedBadges() {
        badgeRepository.save(org.hartford.iqsure.entity.Badge.builder().name("Quick Learner").description("Complete your first quiz").reqPoints(100).icon("🎓").build());
        badgeRepository.save(org.hartford.iqsure.entity.Badge.builder().name("Insurance Pro").description("Score 100% in any quiz").reqPoints(300).icon("🔍").build());
        badgeRepository.save(org.hartford.iqsure.entity.Badge.builder().name("Loyal Member").description("Hold an active policy for 1 month").reqPoints(500).icon("🛡️").build());
        badgeRepository.save(org.hartford.iqsure.entity.Badge.builder().name("Claim Hero").description("Successfully settle your first claim").reqPoints(1000).icon("🏆").build());
        log.info("Badges seeded.");
    }

    private void seedRewards() {
        java.time.LocalDate nextYear = java.time.LocalDate.now().plusYears(1);
        rewardRepository.save(org.hartford.iqsure.entity.Reward.builder().rewardType("CASHBACK").discountValue(10.0).reqPoints(200).description("10% Cashback on next premium pulse").expiryDate(nextYear).build());
        rewardRepository.save(org.hartford.iqsure.entity.Reward.builder().rewardType("DISCOUNT").discountValue(15.0).reqPoints(400).description("15% Discount on any health policy").expiryDate(nextYear).build());
        rewardRepository.save(org.hartford.iqsure.entity.Reward.builder().rewardType("GIFT_CARD").discountValue(500.0).reqPoints(600).description("₹500 Health Pharmacy Gift Card").expiryDate(nextYear).build());
        log.info("Rewards seeded.");
    }

    private void seedDiscountRules() {
        discountRuleRepository.save(org.hartford.iqsure.entity.DiscountRule.builder()
                .ruleName("Beginner Learner Discount")
                .description("Get 5% off for starting your learning journey. Req: 100 points.")
                .minUserPoints(100)
                .discountPercentage(5.0)
                .isActive(true)
                .build());

        discountRuleRepository.save(org.hartford.iqsure.entity.DiscountRule.builder()
                .ruleName("Quiz Whiz Reward")
                .description("Get 10% off for scoring 80%+ on any quiz and having 200 points.")
                .minQuizScorePercent(80.0)
                .minUserPoints(200)
                .discountPercentage(10.0)
                .isActive(true)
                .build());

        discountRuleRepository.save(org.hartford.iqsure.entity.DiscountRule.builder()
                .ruleName("Badge Collector Bonus")
                .description("Get 15% off for earning 3 badges and 500 points.")
                .minBadgesEarned(3)
                .minUserPoints(500)
                .discountPercentage(15.0)
                .isActive(true)
                .build());

        discountRuleRepository.save(org.hartford.iqsure.entity.DiscountRule.builder()
                .ruleName("Health Policy Expert")
                .description("Get 20% off Health Policies. Req: 90% quiz score.")
                .minQuizScorePercent(90.0)
                .applicablePolicyType(org.hartford.iqsure.entity.Policy.PolicyType.HEALTH)
                .discountPercentage(20.0)
                .isActive(true)
                .build());

        discountRuleRepository.save(org.hartford.iqsure.entity.DiscountRule.builder()
                .ruleName("Elite Insurance Protector")
                .description("Top-tier 25% discount for 1000+ points and all 4 badges.")
                .minUserPoints(1000)
                .minBadgesEarned(4)
                .discountPercentage(25.0)
                .isActive(true)
                .build());

        log.info("Discount rules seeded.");
    }
}

