/*
 * FILE: RewardService.java | LOCATION: service/
 * PURPOSE: Reward management. Rewards are auto-granted when users qualify for discount rules.
 *          getEarnedRewardsForUser inlines the check-and-grant logic directly (avoids Spring
 *          proxy issue where @Transactional on internal method calls doesn't propagate).
 * CALLED BY: RewardController.java
 */
package org.hartford.iqsure.service;

import lombok.RequiredArgsConstructor;
import org.hartford.iqsure.dto.request.RewardRequestDTO;
import org.hartford.iqsure.dto.response.RewardResponseDTO;
import org.hartford.iqsure.dto.response.UserRewardResponseDTO;
import org.hartford.iqsure.entity.User;
import org.hartford.iqsure.entity.Reward;
import org.hartford.iqsure.entity.UserReward;
import org.hartford.iqsure.entity.DiscountRule;
import org.hartford.iqsure.exception.BadRequestException;
import org.hartford.iqsure.exception.ResourceNotFoundException;
import org.hartford.iqsure.repository.RewardRepository;
import org.hartford.iqsure.repository.UserRepository;
import org.hartford.iqsure.repository.UserRewardRepository;
import org.hartford.iqsure.repository.UserBadgeRepository;
import org.hartford.iqsure.repository.DiscountRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final UserRepository userRepository;
    private final UserRewardRepository userRewardRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final DiscountRuleRepository discountRuleRepository;

    public RewardResponseDTO createReward(RewardRequestDTO dto) {
        Reward reward = Reward.builder()
                .rewardType(dto.getRewardType())
                .discountValue(dto.getDiscountValue())
                .expiryDate(dto.getExpiryDate())
                .build();
        return toDTO(rewardRepository.save(reward));
    }

    public List<RewardResponseDTO> getAllRewards() {
        return rewardRepository.findAll().stream().map(this::toDTO).toList();
    }

    public List<RewardResponseDTO> getActiveRewards() {
        return rewardRepository.findByExpiryDateAfter(LocalDate.now())
                .stream().map(this::toDTO).toList();
    }

    public List<RewardResponseDTO> getRewardsByUser(Long userId) {
        return userRewardRepository.findByUser_UserId(userId)
                .stream().map(ur -> toDTO(ur.getReward())).toList();
    }

    /**
     * Called when the Rewards page loads.
     * Checks all active discount rules and grants rewards to the user if they qualify.
     * Returns the list of all rewards earned by the user.
     */
    @Transactional
    public List<UserRewardResponseDTO> getEarnedRewardsForUser(Long userId) {
        // Load user stats
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        int userPoints = user.getUserPoints();
        int badgeCount = userBadgeRepository.findByUser_UserId(userId).size();

        // Check active discount rules
        List<DiscountRule> rules = discountRuleRepository.findByIsActiveTrue();

        for (DiscountRule rule : rules) {
            if (rule.getDiscountPercentage() == null) continue;

            boolean qualifies = (rule.getMinUserPoints() <= 0 || userPoints >= rule.getMinUserPoints()) &&
                               (rule.getMinBadgesEarned() <= 0 || badgeCount >= rule.getMinBadgesEarned());

            if (!qualifies) continue;

            // Grant the reward if not already owned
            String label = "Discount: " + rule.getRuleName();

            List<UserReward> currentOwned = userRewardRepository.findByUser_UserId(userId);
            boolean alreadyHas = currentOwned.stream()
                    .anyMatch(ur -> ur.getReward().getRewardType().equals(label));

            if (!alreadyHas) {
                Reward reward = rewardRepository.save(Reward.builder()
                        .rewardType(label)
                        .discountValue(rule.getDiscountPercentage())
                        .expiryDate(LocalDate.now().plusMonths(6))
                        .build());

                userRewardRepository.save(UserReward.builder()
                        .user(user)
                        .reward(reward)
                        .redeemedDate(LocalDateTime.now())
                        .build());
            }
        }

        // Return up-to-date list
        return userRewardRepository.findByUser_UserId(userId)
                .stream()
                .map(ur -> UserRewardResponseDTO.builder()
                        .userRewardId(ur.getId())
                        .rewardTitle(ur.getReward().getRewardType())
                        .rewardType(ur.getReward().getRewardType())
                        .discountValue(ur.getReward().getDiscountValue())
                        .expiryDate(ur.getReward().getExpiryDate().atStartOfDay())
                        .earnedOn(ur.getRedeemedDate())
                        .used(ur.isUsed())
                        .isExpired(ur.getReward().getExpiryDate().isBefore(LocalDate.now()))
                        .build())
                .toList();
    }

    public RewardResponseDTO redeemReward(Long userId, Long rewardId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Reward not found: " + rewardId));

        if (reward.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("This reward has expired");
        }

        if (userRewardRepository.existsByUser_UserIdAndReward_RewardId(userId, rewardId)) {
            throw new BadRequestException("You have already redeemed this reward");
        }

        UserReward userReward = UserReward.builder()
                .user(user)
                .reward(reward)
                .redeemedDate(LocalDateTime.now())
                .build();

        userRewardRepository.save(userReward);
        return toDTO(reward);
    }

    public void deleteReward(Long rewardId) {
        if (!rewardRepository.existsById(rewardId)) {
            throw new ResourceNotFoundException("Reward not found: " + rewardId);
        }
        rewardRepository.deleteById(rewardId);
    }

    private RewardResponseDTO toDTO(Reward r) {
        return RewardResponseDTO.builder()
                .rewardId(r.getRewardId())
                .rewardType(r.getRewardType())
                .discountValue(r.getDiscountValue())
                .expiryDate(r.getExpiryDate())
                .build();
    }
}
