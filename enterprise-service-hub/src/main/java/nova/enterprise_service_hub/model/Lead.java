package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lead entity — captures inbound prospects from the contact form.
 * <p>
 * AI Lead Scoring ranks each lead 0–100 based on budget, project
 * description quality, and company size to prioritize high-value deals.
 */
@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_lead_tenant", columnList = "tenant_id"),
        @Index(name = "idx_lead_status", columnList = "status"),
        @Index(name = "idx_lead_score", columnList = "score"),
        @Index(name = "idx_lead_email", columnList = "email")
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Lead implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    // ── Contact Info ─────────────────────────────────────────────────────
    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Email
    @NotBlank(message = "Email is required")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String email;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

    @Size(max = 200)
    @Column(name = "company_name", length = 200)
    private String companyName;

    // ── Project Details ──────────────────────────────────────────────────
    @Size(max = 200)
    @Column(name = "project_title", length = 200)
    private String projectTitle;

    @Size(max = 10000)
    @Column(name = "project_description", columnDefinition = "TEXT")
    private String projectDescription;

    @Column(precision = 12, scale = 2)
    private BigDecimal budget;

    @Size(max = 50)
    @Column(length = 50)
    private String timeline;

    // ── AI Scoring ───────────────────────────────────────────────────────
    @Column(nullable = false)
    private int score = 0;

    @Size(max = 1000)
    @Column(name = "score_breakdown", length = 1000)
    private String scoreBreakdown;

    // ── Status ───────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadStatus status = LeadStatus.NEW;

    @Size(max = 5000)
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assigned_to", length = 150)
    private String assignedTo;

    // ── Timestamps ───────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum LeadStatus {
        NEW,
        CONTACTED,
        QUALIFIED,
        PROPOSAL_SENT,
        WON,
        LOST
    }
}
