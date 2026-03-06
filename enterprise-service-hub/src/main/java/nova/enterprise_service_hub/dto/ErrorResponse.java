package nova.enterprise_service_hub.dto;

import java.time.Instant;

/**
 * Standardized error response for the "Elite" API.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String timestamp) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, Instant.now().toString());
    }
}
