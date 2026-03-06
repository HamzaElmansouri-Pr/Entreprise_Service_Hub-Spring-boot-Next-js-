package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.SkillNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link SkillNode} entity.
 */
@Repository
public interface SkillNodeRepository extends JpaRepository<SkillNode, Long> {

    Optional<SkillNode> findByTenantIdAndTechnologyName(String tenantId, String technologyName);

    List<SkillNode> findAllByTenantIdOrderByXpDesc(String tenantId);

    List<SkillNode> findAllByTenantIdAndCategoryOrderByXpDesc(String tenantId, SkillNode.SkillCategory category);

    @Query("SELECT s FROM SkillNode s WHERE s.tenantId = :tenantId AND s.level >= :minLevel ORDER BY s.xp DESC")
    List<SkillNode> findMasteredSkills(@Param("tenantId") String tenantId, @Param("minLevel") int minLevel);

    long countByTenantId(String tenantId);

    @Query("SELECT COALESCE(SUM(s.xp), 0) FROM SkillNode s WHERE s.tenantId = :tenantId")
    int totalXpByTenant(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(s) FROM SkillNode s WHERE s.tenantId = :tenantId AND s.level >= :minLevel")
    long countByTenantIdAndLevelGreaterThanEqual(@Param("tenantId") String tenantId, @Param("minLevel") int minLevel);
}
