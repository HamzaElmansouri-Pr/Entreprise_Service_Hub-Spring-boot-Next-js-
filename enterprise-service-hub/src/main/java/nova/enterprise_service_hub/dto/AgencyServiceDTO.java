package nova.enterprise_service_hub.dto;

import java.time.Instant;

/**
 * Response DTO for Agency Services.
 * Includes displayOrder for portfolio sequencing.
 */
public record AgencyServiceDTO(
        Long id,
        String title,
        String slug,
        String description,
        String iconName,
        ImageMetadataDTO image,
        Integer displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
