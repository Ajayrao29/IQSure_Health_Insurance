// Repository for database operations on RewardRepository

package org.hartford.iqsure.repository;
import org.hartford.iqsure.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
    List<Reward> findByExpiryDateAfter(LocalDate today);
    List<Reward> findByRewardType(String rewardType);
}