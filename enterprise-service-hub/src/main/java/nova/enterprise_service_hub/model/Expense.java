package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nova.enterprise_service_hub.security.TenantAware;
import nova.enterprise_service_hub.security.TenantEntityListener;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Expense entity — tracks operational costs for the business.
 * <p>
 * Categories: CLOUD_HOSTING, SOFTWARE_LICENSE, MARKETING, PAYROLL,
 * OFFICE, HARDWARE, TRAVEL, CONSULTING, OTHER.
 * <p>
 * The Autonomous AI Auditor scans this table daily alongside Invoices
 * to flag anomalies (e.g. duplicate vendor charges, cost spikes).
 */
@Entity
@Audited
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expense_tenant", columnList = "tenant_id"),
        @Index(name = "idx_expense_category", columnList = "category"),
        @Index(name = "idx_expense_date", columnList = "expense_date"),
        @Index(name = "idx_expense_vendor", columnList = "vendor")
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Expense implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(length = 200)
    private String vendor;

    @NotNull
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false)
    private boolean recurring = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum ExpenseCategory {
        CLOUD_HOSTING,
        SOFTWARE_LICENSE,
        MARKETING,
        PAYROLL,
        OFFICE,
        HARDWARE,
        TRAVEL,
        CONSULTING,
        OTHER
    }
}
