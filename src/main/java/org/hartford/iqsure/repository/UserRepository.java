// Repository for database operations on UserRepository

package org.hartford.iqsure.repository;
import org.hartford.iqsure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("SELECT u FROM User u WHERE u.role = 'ROLE_USER' ORDER BY u.userPoints DESC")
    List<User> findTopUsersByPoints();
    List<User> findByRole(User.Role role);
}