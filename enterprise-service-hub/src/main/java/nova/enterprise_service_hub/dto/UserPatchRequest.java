package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserPatchRequest(
        @Size(max = 100) String fullName,
        @Email(message = "Must be a valid email") String email,
        @Size(min = 8, message = "Password must be at least 8 characters") String password,
        Boolean enabled) {
}
