package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {
}
