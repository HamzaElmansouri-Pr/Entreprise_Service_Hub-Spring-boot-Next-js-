package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating/updating an Agency Service.
 */
public record ServiceRequest(
                @NotBlank(message = "Title is required") @Size(min = 2, max = 120) String title,

                @NotBlank(message = "Slug is required") @Size(min = 2, max = 140) @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Invalid slug format") String slug,

                @NotBlank(message = "Description is required") @Size(min = 10, max = 2000) String description,

                @Size(max = 60) String iconName,

                ImageMetadataDTO image,

                Boolean active) {
}
