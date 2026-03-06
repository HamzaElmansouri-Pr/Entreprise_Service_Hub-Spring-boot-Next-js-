package nova.enterprise_service_hub.dto;

import java.time.Instant;
import java.util.Set;

public record WebhookDTO(
        Long id,
        String tenantId,
        String url,
        Set<String> events,
        boolean active,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
