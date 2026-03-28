// Repository for database operations on BadgeRepository

package org.hartford.iqsure.repository;
import org.hartford.iqsure.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {
    List<Badge> findByReqPointsLessThanEqual(Integer points);
}