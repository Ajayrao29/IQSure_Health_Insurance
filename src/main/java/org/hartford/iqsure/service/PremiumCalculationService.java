package org.hartford.iqsure.service;

import lombok.RequiredArgsConstructor;
import org.hartford.iqsure.config.AppConfig;
import org.hartford.iqsure.dto.response.PremiumBreakdownDTO;
import org.hartford.iqsure.dto.response.PremiumCalculationLogResponseDTO;
import org.hartford.iqsure.entity.PremiumCalculationLog;
import org.hartford.iqsure.entity.Policy;
import org.hartford.iqsure.entity.User;
import org.hartford.iqsure.entity.Reward;
import org.hartford.iqsure.entity.UserReward;
import org.hartford.iqsure.exception.ResourceNotFoundException;
import org.hartford.iqsure.repository.UserRepository;
import org.hartford.iqsure.repository.PolicyRepository;
import org.hartford.iqsure.repository.DiscountRuleRepository;
import org.hartford.iqsure.repository.UserBadgeRepository;
import org.hartford.iqsure.repository.UserRewardRepository;
import org.hartford.iqsure.repository.PremiumCalculationLogRepository;
import org.hartford.iqsure.dto.response.UserRewardResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PremiumCalculationService {

    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final DiscountRuleRepository discountRuleRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRewardRepository userRewardRepository;
    private final PremiumCalculationLogRepository logRepository;
    private final AppConfig appConfig;

    @Transactional
    public PremiumBreakdownDTO calculatePremium(Long userId, Long policyId, List<Long> selectedRewardIds) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        // User gamification stats (for audit only)
        int userPoints = user.getUserPoints();
        int badgeCount = userBadgeRepository.findByUser_UserId(userId).size();
        double bestQuizScore = 0.0;

        List<PremiumBreakdownDTO.AppliedDiscountDTO> applied = new ArrayList<>();
        double totalDiscount = 0.0;

        /*
         * ACTUARIAL DISCOUNT ENGINE:
         * 1. Manual Coupons (Redeemed via points)
         * 2. Automatic Rule-Based Discounts (Account status, high quiz proficiency)
         */

        if (selectedRewardIds != null && !selectedRewardIds.isEmpty()) {

            Set<Long> selectedSet = Set.copyOf(selectedRewardIds);

            List<UserReward> allUserRewards =
                    userRewardRepository.findByUser_UserIdAndUsedFalse(userId);

            for (UserReward ur : allUserRewards) {

                if (!selectedSet.contains(ur.getId())) {
                    continue;
                }

                Reward reward = ur.getReward();

                boolean notExpired =
                        !reward.getExpiryDate().isBefore(java.time.LocalDate.now());

                if (notExpired) {

                    applied.add(PremiumBreakdownDTO.AppliedDiscountDTO.builder()
                            .ruleName(reward.getRewardType())
                            .discountPercentage(reward.getDiscountValue())
                            .reason("Coupon applied on purchase")
                            .build());

                    totalDiscount += reward.getDiscountValue();
                }
            }
        }

        /* 
         * 2. AUTOMATIC RULE-BASED DISCOUNTS
         * These are earned through account status and learning proficiency
         */
        final double finalBestQuizScore = bestQuizScore;
        discountRuleRepository.findByIsActiveTrue().stream()
                .filter(rule -> {
                    // Check if rule applies to this policy type
                    if (rule.getApplicablePolicyType() != null && 
                        rule.getApplicablePolicyType() != policy.getPolicyType()) {
                        return false;
                    }
                    // Check criteria
                    return userPoints >= rule.getMinUserPoints() && 
                           badgeCount >= rule.getMinBadgesEarned() && 
                           finalBestQuizScore >= rule.getMinQuizScorePercent();
                })
                .forEach(rule -> {
                    applied.add(PremiumBreakdownDTO.AppliedDiscountDTO.builder()
                            .ruleName(rule.getRuleName())
                            .discountPercentage(rule.getDiscountPercentage())
                            .reason("Automatic qualification - Account Status")
                            .build());
                    // We sum them for now, but cap it later
                });

        totalDiscount = applied.stream()
                .mapToDouble(PremiumBreakdownDTO.AppliedDiscountDTO::getDiscountPercentage)
                .sum();

        // Apply discount cap
        if (totalDiscount > appConfig.getMaxDiscountCap()) {
            totalDiscount = appConfig.getMaxDiscountCap();
        }

        double basePremium = policy.getBasePremium();

        double discountedAmount =
                Math.round(basePremium * (totalDiscount / 100.0) * 100.0) / 100.0;

        double finalPremium =
                Math.round((basePremium - discountedAmount) * 100.0) / 100.0;

        double roundedBestScore =
                Math.round(bestQuizScore * 100.0) / 100.0;

        // Save calculation log
        String ruleNames = applied.stream()
                .map(PremiumBreakdownDTO.AppliedDiscountDTO::getRuleName)
                .collect(Collectors.joining(", "));

        logRepository.save(PremiumCalculationLog.builder()
                .user(user)
                .policy(policy)
                .basePremium(basePremium)
                .totalDiscountPercent(totalDiscount)
                .finalPremium(finalPremium)
                .userPointsSnapshot(userPoints)
                .badgeCountSnapshot(badgeCount)
                .bestQuizScoreSnapshot(roundedBestScore)
                .appliedRuleNames(ruleNames.isEmpty() ? "None" : ruleNames)
                .calculatedAt(LocalDateTime.now())
                .build());

        return PremiumBreakdownDTO.builder()
                .policyId(policy.getPolicyId())
                .policyTitle(policy.getTitle())
                .policyType(policy.getPolicyType())
                .basePremium(basePremium)
                .durationMonths(policy.getDurationMonths())
                .coverageAmount(policy.getCoverageAmount())
                .userId(userId)
                .userPoints(userPoints)
                .badgesEarned(badgeCount)
                .bestQuizScorePercent(roundedBestScore)
                .appliedDiscounts(applied)
                .totalDiscountPercent(totalDiscount)
                .discountedAmount(discountedAmount)
                .finalPremium(finalPremium)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    public List<PremiumCalculationLogResponseDTO> getLogsForUser(Long userId) {

        if (!userRepository.existsById(userId))
            throw new ResourceNotFoundException("User not found: " + userId);

        return logRepository.findByUser_UserIdOrderByCalculatedAtDesc(userId)
                .stream()
                .map(this::toLogDTO)
                .toList();
    }

    public List<PremiumCalculationLogResponseDTO> getLogsForUserAndPolicy(Long userId, Long policyId) {

        if (!userRepository.existsById(userId))
            throw new ResourceNotFoundException("User not found: " + userId);

        if (!policyRepository.existsById(policyId))
            throw new ResourceNotFoundException("Policy not found: " + policyId);

        return logRepository
                .findByUser_UserIdAndPolicy_PolicyIdOrderByCalculatedAtDesc(userId, policyId)
                .stream()
                .map(this::toLogDTO)
                .toList();
    }

    // Mark rewards as used after purchase
    @Transactional
    public void markRewardsAsUsed(List<Long> userRewardIds) {

        if (userRewardIds == null || userRewardIds.isEmpty()) {
            return;
        }

        userRewardRepository.findAllById(userRewardIds).forEach(ur -> {
            ur.setUsed(true);
            userRewardRepository.save(ur);
        });
    }

    /**
     * Retrieves all available (unexpired and unused) coupons for a user.
     * Returns DTOs for the frontend to display.
     */
    public List<UserRewardResponseDTO> getAvailableRewardsForUser(Long userId) {
        return userRewardRepository
                .findByUser_UserIdAndUsedFalse(userId)
                .stream()
                .filter(ur -> !ur.getReward().getExpiryDate().isBefore(java.time.LocalDate.now()))
                .map(this::toUserRewardDTO)
                .toList();
    }

    private UserRewardResponseDTO toUserRewardDTO(UserReward ur) {
        return UserRewardResponseDTO.builder()
                .userRewardId(ur.getId())
                .rewardTitle(ur.getReward().getRewardType()) // Using reward type as title
                .rewardType(ur.getReward().getRewardType())
                .discountValue(ur.getReward().getDiscountValue())
                .expiryDate(ur.getReward().getExpiryDate().atStartOfDay()) // Convert LocalDate to LocalDateTime
                .used(ur.isUsed())
                .build();
    }

    private PremiumCalculationLogResponseDTO toLogDTO(PremiumCalculationLog l) {

        return PremiumCalculationLogResponseDTO.builder()
                .logId(l.getLogId())
                .userId(l.getUser().getUserId())
                .policyId(l.getPolicy().getPolicyId())
                .policyTitle(l.getPolicy().getTitle())
                .basePremium(l.getBasePremium())
                .totalDiscountPercent(l.getTotalDiscountPercent())
                .finalPremium(l.getFinalPremium())
                .userPointsSnapshot(l.getUserPointsSnapshot())
                .badgeCountSnapshot(l.getBadgeCountSnapshot())
                .bestQuizScoreSnapshot(l.getBestQuizScoreSnapshot())
                .appliedRuleNames(l.getAppliedRuleNames())
                .calculatedAt(l.getCalculatedAt())
                .build();
    }
}