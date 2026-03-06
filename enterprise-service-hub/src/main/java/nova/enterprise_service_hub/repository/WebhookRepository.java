package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    List<Webhook> findByTenantIdAndActiveTrue(String tenantId);

    List<Webhook> findByTenantId(String tenantId);
}
