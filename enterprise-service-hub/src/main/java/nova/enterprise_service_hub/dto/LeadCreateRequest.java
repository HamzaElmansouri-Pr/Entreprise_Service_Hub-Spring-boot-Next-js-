package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LeadCreateRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150)
        String fullName,

        @Email
        @NotBlank(message = "Email is required")
        @Size(max = 200)
        String email,

        @Size(max = 30)
        String phone,

        @Size(max = 200)
        String companyName,

        @Size(max = 200)
        String projectTitle,

        @Size(max = 10000)
        String projectDescription,

        BigDecimal budget,

        @Size(max = 50)
        String timeline) {
}
