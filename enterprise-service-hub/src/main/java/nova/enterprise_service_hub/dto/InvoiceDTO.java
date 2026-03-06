package nova.enterprise_service_hub.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InvoiceDTO(
        Long id,
        String referenceNumber,
        BigDecimal amount,
        String status,
        LocalDate dueDate,
        Long projectId,
        String projectName,
        Instant createdAt,
        Instant updatedAt) {
}
