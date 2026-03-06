package nova.enterprise_service_hub.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProposalDTO(
        Long id,
        String proposalNumber,
        Long leadId,
        String leadName,
        String leadEmail,
        String title,
        String scopeOfWork,
        String deliverables,
        String termsAndConditions,
        List<LineItemDTO> lineItems,
        BigDecimal totalAmount,
        int depositPercent,
        String currency,
        LocalDate validUntil,
        String status,
        String signerName,
        String signerEmail,
        Instant signedAt,
        Long convertedProjectId,
        Long depositInvoiceId,
        String signingToken,
        Instant createdAt,
        Instant updatedAt) {

    public record LineItemDTO(
            Long id,
            String description,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            int displayOrder) {
    }
}
