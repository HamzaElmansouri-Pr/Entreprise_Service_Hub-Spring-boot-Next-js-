package nova.enterprise_service_hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nova.enterprise_service_hub.dto.WebhookCreateRequest;
import nova.enterprise_service_hub.dto.WebhookDTO;
import nova.enterprise_service_hub.model.Webhook;
import nova.enterprise_service_hub.model.WebhookDelivery;
import nova.enterprise_service_hub.repository.WebhookDeliveryRepository;
import nova.enterprise_service_hub.repository.WebhookRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Webhook Service — Phase 2.3
 * <p>
 * Registers webhook URLs per tenant and fires HTTP callbacks
 * for events like invoice.created, project.updated, etc.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebhookService(WebhookRepository webhookRepository,
                          WebhookDeliveryRepository deliveryRepository,
                          ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<WebhookDTO> getWebhooksForCurrentTenant() {
        String tenantId = TenantContext.getTenantId();
        return webhookRepository.findByTenantId(tenantId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public WebhookDTO createWebhook(WebhookCreateRequest request) {
        Webhook webhook = new Webhook();
        webhook.setTenantId(TenantContext.getTenantId());
        webhook.setUrl(request.url());
        webhook.setEvents(request.events());
        webhook.setDescription(request.description());
        webhook.setSecret(request.secret());
        return toDTO(webhookRepository.save(webhook));
    }

    @Transactional
    public void deleteWebhook(Long id) {
        Webhook webhook = webhookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Webhook not found: " + id));
        webhookRepository.delete(webhook);
    }

    @Transactional
    public WebhookDTO toggleWebhook(Long id) {
        Webhook webhook = webhookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Webhook not found: " + id));
        webhook.setActive(!webhook.isActive());
        return toDTO(webhookRepository.save(webhook));
    }

    /**
     * Fire a webhook event asynchronously to all registered listeners.
     */
    @Async
    public void fireEvent(String tenantId, String eventType, Object payload) {
        List<Webhook> hooks = webhookRepository.findByTenantIdAndActiveTrue(tenantId);
        for (Webhook hook : hooks) {
            if (hook.getEvents().contains(eventType)) {
                deliverWebhook(hook, eventType, payload);
            }
        }
    }

    private void deliverWebhook(Webhook webhook, String eventType, Object payload) {
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setWebhook(webhook);
        delivery.setEventType(eventType);

        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "event", eventType,
                    "data", payload,
                    "timestamp", java.time.Instant.now().toString()
            ));
            delivery.setPayload(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhook.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Event", eventType)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (webhook.getSecret() != null && !webhook.getSecret().isBlank()) {
                requestBuilder.header("X-Webhook-Secret", webhook.getSecret());
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            delivery.setResponseStatus(response.statusCode());
            delivery.setResponseBody(response.body().substring(0, Math.min(response.body().length(), 2000)));
            delivery.setStatus(response.statusCode() < 400
                    ? WebhookDelivery.DeliveryStatus.SUCCESS
                    : WebhookDelivery.DeliveryStatus.FAILED);
            delivery.setAttemptCount(1);

        } catch (Exception e) {
            log.error("Webhook delivery failed for hook {} event {}: {}", webhook.getId(), eventType, e.getMessage());
            delivery.setStatus(WebhookDelivery.DeliveryStatus.FAILED);
            delivery.setResponseBody(e.getMessage());
            delivery.setAttemptCount(1);
        }

        deliveryRepository.save(delivery);
    }

    private WebhookDTO toDTO(Webhook w) {
        return new WebhookDTO(w.getId(), w.getTenantId(), w.getUrl(),
                w.getEvents(), w.isActive(), w.getDescription(),
                w.getCreatedAt(), w.getUpdatedAt());
    }
}
