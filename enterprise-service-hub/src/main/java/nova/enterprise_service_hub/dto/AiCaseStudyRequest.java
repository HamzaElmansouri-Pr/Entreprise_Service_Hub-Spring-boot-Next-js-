package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for AI case study generation.
 * <p>
 * The admin pastes raw project notes and the LLM structures them
 * into the {@link CaseStudyDTO} format the frontend expects.
 */
public record AiCaseStudyRequest(

        @NotBlank(message = "Project name is required")
        @Size(max = 200)
        String projectName,

        @NotBlank(message = "Client name is required")
        @Size(max = 200)
        String clientName,

        @NotBlank(message = "Raw notes are required")
        @Size(max = 5000, message = "Notes must not exceed 5000 characters")
        String rawNotes) {
}
