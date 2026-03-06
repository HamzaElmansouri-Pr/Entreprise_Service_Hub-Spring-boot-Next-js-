package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.WebhookCreateRequest;
import nova.enterprise_service_hub.dto.WebhookDTO;
import nova.enterprise_service_hub.service.WebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Webhook Controller — Phase 2.3
 * <p>
 * CRUD for tenant webhook registrations.
 */
@RestController
@RequestMapping("/v1/webhooks")
@PreAuthorize("hasRole('ADMIN')")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<List<WebhookDTO>> list() {
        return ResponseEntity.ok(webhookService.getWebhooksForCurrentTenant());
    }

    @PostMapping
    public ResponseEntity<WebhookDTO> create(@Valid @RequestBody WebhookCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(webhookService.createWebhook(request));
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<WebhookDTO> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(webhookService.toggleWebhook(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
        return ResponseEntity.noContent().build();
    }
}
