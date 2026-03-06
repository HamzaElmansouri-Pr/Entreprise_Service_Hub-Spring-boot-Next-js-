package nova.enterprise_service_hub.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for Portfolio Projects.
 * Includes displayOrder, archived status, and structured case study + image.
 */
public record ProjectDTO(
        Long id,
        String name,
        String clientName,
        CaseStudyDTO caseStudy,
        ImageMetadataDTO image,
        Integer displayOrder,
        boolean archived,
        List<String> technologies,
        Instant createdAt,
        Instant updatedAt) {
}
