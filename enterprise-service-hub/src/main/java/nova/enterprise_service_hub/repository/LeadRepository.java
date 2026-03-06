package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByTenantIdOrderByScoreDesc(String tenantId);

    Page<Lead> findByTenantId(String tenantId, Pageable pageable);

    List<Lead> findByTenantIdAndStatus(String tenantId, Lead.LeadStatus status);

    Optional<Lead> findByIdAndTenantId(Long id, String tenantId);

    Optional<Lead> findByEmailAndTenantId(String email, String tenantId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, Lead.LeadStatus status);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.score >= :minScore ORDER BY l.score DESC")
    List<Lead> findHotLeads(@Param("tenantId") String tenantId, @Param("minScore") int minScore);
}
