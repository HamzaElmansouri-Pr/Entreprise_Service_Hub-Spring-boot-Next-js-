package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO for creating/updating a Portfolio Project.
 * Case study is accepted as three structured sections.
 */
public record ProjectRequest(
                @NotBlank(message = "Project name is required") @Size(min = 2, max = 150) String name,

                @NotBlank(message = "Client name is required") @Size(min = 2, max = 150) String clientName,

                @Size(max = 5000) String caseStudyChallenge,
                @Size(max = 5000) String caseStudySolution,
                @Size(max = 5000) String caseStudyResult,

                @Size(max = 500) String previewUrl,
                ImageMetadataDTO image,
                List<ImageMetadataDTO> gallery,

                List<String> technologies) {
}
