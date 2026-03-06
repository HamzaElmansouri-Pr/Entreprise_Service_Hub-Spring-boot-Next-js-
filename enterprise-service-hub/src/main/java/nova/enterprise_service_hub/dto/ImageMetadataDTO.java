package nova.enterprise_service_hub.dto;

/**
 * DTO for image metadata in API requests and responses.
 */
public record ImageMetadataDTO(
        String url,
        String altText,
        Integer width,
        Integer height) {
}
