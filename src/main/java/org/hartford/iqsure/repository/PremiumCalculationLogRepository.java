// Repository for database operations on PremiumCalculationLogRepository

package org.hartford.iqsure.repository;
import org.hartford.iqsure.entity.PremiumCalculationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface PremiumCalculationLogRepository extends JpaRepository<PremiumCalculationLog, Long> {
    List<PremiumCalculationLog> findByUser_UserIdOrderByCalculatedAtDesc(Long userId);
    List<PremiumCalculationLog> findByUser_UserIdAndPolicy_PolicyIdOrderByCalculatedAtDesc(Long userId, Long policyId);
}