package nova.enterprise_service_hub.dto;

/**
 * DTO for PATCH /api/v1/services/{id} — partial updates.
 * All fields are optional. Only non-null fields are applied.
 */
public record ServicePatchRequest(
        String title,
        String slug,
        String description,
        String iconName,
        ImageMetadataDTO image,
        Integer displayOrder,
        Boolean active) {
}
