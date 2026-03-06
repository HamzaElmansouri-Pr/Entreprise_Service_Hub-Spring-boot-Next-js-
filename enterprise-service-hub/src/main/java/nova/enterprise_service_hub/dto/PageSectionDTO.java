package nova.enterprise_service_hub.dto;

import java.time.Instant;
import java.util.Map;

public record PageSectionDTO(
        Long id,
        String pageName,
        String sectionKey,
        String title,
        String description,
        Map<String, Object> contentData,
        Integer displayOrder,
        ImageMetadataDTO image,
        Instant createdAt,
        Instant updatedAt) {
}
