package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseCreateRequest(
        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotNull(message = "Category is required")
        String category,

        String vendor,

        @NotNull(message = "Expense date is required")
        LocalDate expenseDate,

        String referenceNumber,
        String notes,
        boolean recurring) {
}
