package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProposalCreateRequest(
        @NotNull(message = "Lead ID is required")
        Long leadId,

        @NotBlank(message = "Title is required")
        @Size(max = 200)
        String title,

        @Size(max = 20000)
        String scopeOfWork,

        @Size(max = 10000)
        String deliverables,

        @Size(max = 5000)
        String termsAndConditions,

        List<LineItemRequest> lineItems,

        int depositPercent,

        String currency,

        LocalDate validUntil) {

    public record LineItemRequest(
            @NotBlank String description,
            int quantity,
            @NotNull BigDecimal unitPrice,
            int displayOrder) {
    }
}
