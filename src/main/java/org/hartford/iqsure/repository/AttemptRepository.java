// Repository for database operations on AttemptRepository
package org.hartford.iqsure.repository;
import org.hartford.iqsure.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByUser_UserIdOrderByAttemptDateDesc(Long userId);
}