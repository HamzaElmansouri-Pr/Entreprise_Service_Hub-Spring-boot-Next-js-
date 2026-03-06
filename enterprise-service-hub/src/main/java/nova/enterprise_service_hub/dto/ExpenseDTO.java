package nova.enterprise_service_hub.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseDTO(
        Long id,
        String description,
        BigDecimal amount,
        String category,
        String vendor,
        LocalDate expenseDate,
        String referenceNumber,
        String notes,
        boolean recurring,
        Instant createdAt,
        Instant updatedAt) {
}
