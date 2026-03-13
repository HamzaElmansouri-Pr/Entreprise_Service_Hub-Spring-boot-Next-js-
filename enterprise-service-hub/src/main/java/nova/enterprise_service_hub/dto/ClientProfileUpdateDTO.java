package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.Size;

public record ClientProfileUpdateDTO(
        @Size(min = 2, max = 150)
        String fullName,

        @Size(min = 8, max = 120, message = "Password must be at least 8 characters")
        String password, // Optional, only if changing

        String phoneNumber) {
}
