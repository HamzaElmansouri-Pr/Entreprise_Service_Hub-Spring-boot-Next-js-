package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Badge} entity.
 */
@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findAllByUserIdOrderByEarnedAtDesc(Long userId);

    List<Badge> findAllByTenantIdOrderByEarnedAtDesc(String tenantId);

    boolean existsByUserIdAndBadgeType(Long userId, Badge.BadgeType badgeType);

    long countByUserId(Long userId);

    long countByTenantId(String tenantId);
}
