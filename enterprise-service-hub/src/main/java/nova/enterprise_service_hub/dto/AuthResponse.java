package nova.enterprise_service_hub.dto;

import java.util.Set;

/**
 * DTO for returning authentication tokens and basic user info.
 */
public record AuthResponse(
                String token,
                String refreshToken,
                long expiresIn,
                String email,
                String fullName,
                Set<String> roles) {
}
