package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.AiCaseStudyRequest;
import nova.enterprise_service_hub.dto.AiSeoRequest;
import nova.enterprise_service_hub.dto.AiSeoResponse;
import nova.enterprise_service_hub.dto.CaseStudyDTO;
import nova.enterprise_service_hub.service.AiContentService;
import nova.enterprise_service_hub.service.FeatureGateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for AI-powered content generation.
 * <p>
 * All endpoints are restricted to {@code ROLE_ADMIN} — only
 * authenticated agency admins can trigger LLM generation.
 * Responses are {@link CompletableFuture}-backed for non-blocking execution.
 */
@RestController
@RequestMapping("/v1/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AiContentController {

    private final AiContentService aiContentService;
    private final FeatureGateService featureGateService;

    public AiContentController(AiContentService aiContentService, FeatureGateService featureGateService) {
        this.aiContentService = aiContentService;
        this.featureGateService = featureGateService;
    }

    /**
     * POST /api/v1/ai/case-study — Generate a structured case study
     * from raw project notes.
     * <p>
     * Returns: {@link CaseStudyDTO} with challenge, solution, result.
     */
    @PostMapping("/case-study")
    public CompletableFuture<ResponseEntity<CaseStudyDTO>> generateCaseStudy(
            @Valid @RequestBody AiCaseStudyRequest request) {
        featureGateService.requireModule("ai_content");
        return aiContentService.generateCaseStudy(request)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * POST /api/v1/ai/seo — Generate SEO meta-title and meta-description
     * for a service page.
     * <p>
     * Returns: {@link AiSeoResponse} with metaTitle, metaDescription.
     */
    @PostMapping("/seo")
    public CompletableFuture<ResponseEntity<AiSeoResponse>> generateSeoMetadata(
            @Valid @RequestBody AiSeoRequest request) {
        featureGateService.requireModule("ai_content");
        return aiContentService.generateSeoMetadata(request)
                .thenApply(ResponseEntity::ok);
    }
}
