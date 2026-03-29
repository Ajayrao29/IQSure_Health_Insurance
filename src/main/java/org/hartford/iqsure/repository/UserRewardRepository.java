// Repository for database operations on UserRewardRepository

package org.hartford.iqsure.repository;
import org.hartford.iqsure.entity.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface UserRewardRepository extends JpaRepository<UserReward, Long> {
    List<UserReward> findByUser_UserId(Long userId);
    List<UserReward> findByUser_UserIdAndUsedFalse(Long userId);
    boolean existsByUser_UserIdAndReward_RewardId(Long userId, Long rewardId);
    boolean existsByReward_RewardId(Long rewardId);
}