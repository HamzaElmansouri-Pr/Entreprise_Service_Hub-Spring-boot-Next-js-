package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.SprintSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link SprintSession} entity.
 */
@Repository
public interface SprintSessionRepository extends JpaRepository<SprintSession, Long> {

    Optional<SprintSession> findByUserIdAndStatus(Long userId, SprintSession.SprintStatus status);

    Page<SprintSession> findAllByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    List<SprintSession> findAllByUserIdAndStatusOrderByStartedAtDesc(Long userId, SprintSession.SprintStatus status);

    @Query("SELECT COUNT(s) FROM SprintSession s WHERE s.userId = :userId AND s.status = 'COMPLETED'")
    long countCompletedByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) FROM SprintSession s WHERE s.userId = :userId AND s.status = 'COMPLETED' AND s.startedAt >= :since")
    long countCompletedByUserSince(@Param("userId") Long userId, @Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(s.actualMinutes), 0) FROM SprintSession s WHERE s.userId = :userId AND s.status = 'COMPLETED'")
    int totalFocusMinutesByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(AVG(s.focusScore), 0) FROM SprintSession s WHERE s.userId = :userId AND s.status = 'COMPLETED'")
    double averageFocusScoreByUser(@Param("userId") Long userId);

    @Query("SELECT s FROM SprintSession s WHERE s.userId = :userId AND s.status = 'COMPLETED' AND s.startedAt >= :since ORDER BY s.startedAt DESC")
    List<SprintSession> findRecentCompleted(@Param("userId") Long userId, @Param("since") Instant since);
}
