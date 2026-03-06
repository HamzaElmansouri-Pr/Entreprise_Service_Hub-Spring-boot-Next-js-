package nova.enterprise_service_hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nova.enterprise_service_hub.dto.AiCaseStudyRequest;
import nova.enterprise_service_hub.dto.AiSeoRequest;
import nova.enterprise_service_hub.dto.AiSeoResponse;
import nova.enterprise_service_hub.dto.CaseStudyDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI Content Service — Orchestrates OpenAI GPT calls for structured
 * content generation.
 * <p>
 * All public methods are {@code @Async} to keep the admin UI responsive.
 * Returns {@link CompletableFuture} so the controller can stream results
 * back without blocking servlet threads.
 *
 * <h3>Capabilities</h3>
 * <ul>
 *   <li><strong>Case Study Generation</strong> — Transforms raw project notes
 *       into a structured {@link CaseStudyDTO} (Challenge / Solution / Result)
 *       in the exact JSON format the frontend expects.</li>
 *   <li><strong>SEO Metadata Generation</strong> — Produces optimized
 *       meta-titles (≤60 chars) and meta-descriptions (≤160 chars) from
 *       service titles and industry keywords.</li>
 * </ul>
 */
@Service
public class AiContentService {

    private static final Logger log = LoggerFactory.getLogger(AiContentService.class);

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.openai.model}")
    private String model;

    @Value("${ai.openai.max-tokens}")
    private int maxTokens;

    @Value("${ai.openai.temperature}")
    private double temperature;

    public AiContentService(@Qualifier("openAiRestClient") RestClient openAiRestClient,
                            ObjectMapper objectMapper) {
        this.openAiRestClient = openAiRestClient;
        this.objectMapper = objectMapper;
    }

    // ── Case Study Generation ───────────────────────────────────────────────

    /**
     * Generates a structured case study from raw project notes.
     * Runs asynchronously on the {@code aiTaskExecutor} pool.
     *
     * @param request raw notes with project/client context
     * @return a {@link CompletableFuture} resolving to a {@link CaseStudyDTO}
     */
    @Async("aiTaskExecutor")
    public CompletableFuture<CaseStudyDTO> generateCaseStudy(AiCaseStudyRequest request) {
        log.info("AI | Generating case study for project '{}' (client: {})",
                request.projectName(), request.clientName());

        String systemPrompt = """
                You are a senior B2B copywriter at an elite digital agency.
                Your task is to transform raw project notes into a polished, structured case study.
                
                RULES:
                - Output ONLY valid JSON, no markdown, no explanation, no wrapping.
                - Use the exact keys: "challenge", "solution", "result".
                - Each section should be 2-4 concise, impactful sentences.
                - Write in third-person professional tone.
                - Quantify results with metrics when raw notes provide them.
                - If metrics are absent, describe qualitative impact.
                
                OUTPUT FORMAT (strict JSON):
                {
                  "challenge": "...",
                  "solution": "...",
                  "result": "..."
                }
                """;

        String userPrompt = """
                Project: %s
                Client: %s
                
                Raw Notes:
                %s
                """.formatted(request.projectName(), request.clientName(), request.rawNotes());

        String response = callChatCompletion(systemPrompt, userPrompt);
        CaseStudyDTO caseStudy = parseCaseStudy(response);

        log.info("AI | Case study generated successfully for '{}'", request.projectName());
        return CompletableFuture.completedFuture(caseStudy);
    }

    // ── SEO Metadata Generation ─────────────────────────────────────────────

    /**
     * Generates SEO-optimized meta-title and meta-description.
     * Runs asynchronously on the {@code aiTaskExecutor} pool.
     *
     * @param request service title, description, and industry keywords
     * @return a {@link CompletableFuture} resolving to {@link AiSeoResponse}
     */
    @Async("aiTaskExecutor")
    public CompletableFuture<AiSeoResponse> generateSeoMetadata(AiSeoRequest request) {
        log.info("AI | Generating SEO metadata for service '{}'", request.serviceTitle());

        String systemPrompt = """
                You are an SEO specialist at a top-tier digital agency.
                Generate an optimized meta-title and meta-description for a service page.
                
                RULES:
                - Output ONLY valid JSON, no markdown, no explanation, no wrapping.
                - meta_title: max 60 characters, include primary keyword, compelling.
                - meta_description: max 160 characters, include a call-to-action, natural keyword usage.
                - Write for B2B decision-makers and technical leads.
                - Incorporate provided industry keywords naturally.
                
                OUTPUT FORMAT (strict JSON):
                {
                  "meta_title": "...",
                  "meta_description": "..."
                }
                """;

        String userPrompt = buildSeoUserPrompt(request);

        String response = callChatCompletion(systemPrompt, userPrompt);
        AiSeoResponse seoResponse = parseSeoResponse(response);

        log.info("AI | SEO metadata generated for '{}'", request.serviceTitle());
        return CompletableFuture.completedFuture(seoResponse);
    }

    // ── OpenAI HTTP Call ────────────────────────────────────────────────────

    private String callChatCompletion(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", temperature,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        String responseJson = openAiRestClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractContent(responseJson);
    }

    // ── JSON Parsing Helpers ────────────────────────────────────────────────

    private String extractContent(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("AI | Failed to parse OpenAI response: {}", e.getMessage());
            throw new AiContentException("Failed to parse OpenAI response", e);
        }
    }

    private CaseStudyDTO parseCaseStudy(String json) {
        try {
            String cleaned = cleanJsonResponse(json);
            JsonNode node = objectMapper.readTree(cleaned);
            return new CaseStudyDTO(
                    node.path("challenge").asText(),
                    node.path("solution").asText(),
                    node.path("result").asText());
        } catch (Exception e) {
            log.error("AI | Failed to parse case study JSON: {}", e.getMessage());
            throw new AiContentException("Failed to parse AI-generated case study", e);
        }
    }

    private AiSeoResponse parseSeoResponse(String json) {
        try {
            String cleaned = cleanJsonResponse(json);
            JsonNode node = objectMapper.readTree(cleaned);
            return new AiSeoResponse(
                    node.path("meta_title").asText(),
                    node.path("meta_description").asText());
        } catch (Exception e) {
            log.error("AI | Failed to parse SEO JSON: {}", e.getMessage());
            throw new AiContentException("Failed to parse AI-generated SEO metadata", e);
        }
    }

    /**
     * Strips markdown code fences that LLMs sometimes wrap around JSON output.
     */
    private String cleanJsonResponse(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private String buildSeoUserPrompt(AiSeoRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Service Title: ").append(request.serviceTitle()).append("\n");
        if (request.serviceDescription() != null && !request.serviceDescription().isBlank()) {
            sb.append("Service Description: ").append(request.serviceDescription()).append("\n");
        }
        if (request.industryKeywords() != null && !request.industryKeywords().isBlank()) {
            sb.append("Industry Keywords: ").append(request.industryKeywords()).append("\n");
        }
        return sb.toString();
    }

    // ── Custom Exception ────────────────────────────────────────────────────

    public static class AiContentException extends RuntimeException {
        public AiContentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
