package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PageSectionPatchRequest(
        @Size(max = 180) String title,
        @Size(max = 2000) String description,
        Map<String, Object> contentData,
        @Min(1) Integer displayOrder,
        ImageMetadataDTO image) {
}
