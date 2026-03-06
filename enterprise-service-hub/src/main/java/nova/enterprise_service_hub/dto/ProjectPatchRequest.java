package nova.enterprise_service_hub.dto;

import java.util.List;

/**
 * DTO for PATCH /api/v1/projects/{id} — partial updates.
 * All fields are optional. Only non-null fields are applied.
 */
public record ProjectPatchRequest(
        String name,
        String clientName,
        String caseStudyChallenge,
        String caseStudySolution,
        String caseStudyResult,
        ImageMetadataDTO image,
        Integer displayOrder,
        Boolean archived,
        List<String> technologies) {
}
