package nova.enterprise_service_hub.dto;

import java.time.Instant;

public record NotificationDTO(
        Long id,
        String title,
        String message,
        String type,
        boolean read,
        String entityType,
        Long entityId,
        Instant createdAt) {
}
