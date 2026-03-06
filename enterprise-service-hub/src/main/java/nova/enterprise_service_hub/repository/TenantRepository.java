package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Tenant;
import nova.enterprise_service_hub.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);

    List<Tenant> findAllByActiveTrue();

    List<Tenant> findAllBySubscriptionPlan(SubscriptionPlan plan);

    long countByActiveTrue();

    @Query("SELECT COUNT(DISTINCT u.tenantId) FROM User u WHERE u.enabled = true")
    long countDistinctActiveTenantUsers();

    @Query("SELECT t FROM Tenant t ORDER BY t.createdAt DESC")
    List<Tenant> findAllOrderByCreatedAtDesc();
}
