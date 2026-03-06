package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record WebhookCreateRequest(
        @NotBlank String url,
        @NotEmpty Set<String> events,
        String description,
        String secret
) {}
