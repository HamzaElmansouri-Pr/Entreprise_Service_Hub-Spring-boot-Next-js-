package nova.enterprise_service_hub.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LeadDTO(
        Long id,
        String fullName,
        String email,
        String phone,
        String companyName,
        String projectTitle,
        String projectDescription,
        BigDecimal budget,
        String timeline,
        int score,
        String scoreBreakdown,
        String status,
        String notes,
        String assignedTo,
        Instant createdAt,
        Instant updatedAt) {
}
