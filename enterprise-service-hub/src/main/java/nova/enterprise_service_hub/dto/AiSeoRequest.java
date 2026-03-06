package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for AI-generated SEO metadata.
 * <p>
 * Takes the service title plus optional industry keywords and
 * produces an optimized meta-title and meta-description.
 */
public record AiSeoRequest(

        @NotBlank(message = "Service title is required")
        @Size(max = 200)
        String serviceTitle,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String serviceDescription,

        @Size(max = 300, message = "Keywords must not exceed 300 characters")
        String industryKeywords) {
}
