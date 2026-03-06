package nova.enterprise_service_hub.dto;

/**
 * Structured Case Study DTO — Breaks the case study into three
 * frontend-friendly sections for rich portfolio rendering.
 */
public record CaseStudyDTO(
        String challenge,
        String solution,
        String result) {
}
