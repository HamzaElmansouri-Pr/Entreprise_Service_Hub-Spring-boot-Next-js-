package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByTenantId(String tenantId);

    List<Subscription> findAllByStatus(Subscription.SubscriptionStatus status);

    boolean existsByTenantId(String tenantId);
}
