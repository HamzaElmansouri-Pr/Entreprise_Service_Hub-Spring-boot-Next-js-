package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findByWebhookIdOrderByCreatedAtDesc(Long webhookId);
}
