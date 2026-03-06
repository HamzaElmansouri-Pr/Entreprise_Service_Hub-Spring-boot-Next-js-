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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Proposal entity — professional proposals generated for qualified leads.
 * <p>
 * Includes line items, digital signature capture, and auto-conversion
 * logic that creates a Project + Deposit Invoice on acceptance.
 */
@Entity
@Table(name = "proposals", indexes = {
        @Index(name = "idx_proposal_tenant", columnList = "tenant_id"),
        @Index(name = "idx_proposal_lead", columnList = "lead_id"),
        @Index(name = "idx_proposal_status", columnList = "status"),
        @Index(name = "idx_proposal_token", columnList = "signing_token", unique = true)
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Proposal implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "proposal_number", unique = true, nullable = false, length = 30)
    private String proposalNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    // ── Content ──────────────────────────────────────────────────────────
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Size(max = 20000)
    @Column(name = "scope_of_work", columnDefinition = "TEXT")
    private String scopeOfWork;

    @Size(max = 10000)
    @Column(columnDefinition = "TEXT")
    private String deliverables;

    @Size(max = 5000)
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    // ── Line Items (serialized JSON for flexibility) ─────────────────────
    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalLineItem> lineItems = new ArrayList<>();

    // ── Financials ───────────────────────────────────────────────────────
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "deposit_percent", nullable = false)
    private int depositPercent = 30;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    @Column(name = "valid_until")
    private LocalDate validUntil;

    // ── Digital Signature ────────────────────────────────────────────────
    @Column(name = "signing_token", unique = true, length = 128)
    private String signingToken;

    @Size(max = 200)
    @Column(name = "signer_name", length = 200)
    private String signerName;

    @Size(max = 200)
    @Column(name = "signer_email", length = 200)
    private String signerEmail;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "signer_ip", length = 45)
    private String signerIp;

    // ── Conversion Link ──────────────────────────────────────────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_project_id")
    private Project convertedProject;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_invoice_id")
    private Invoice depositInvoice;

    // ── Status ───────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status = ProposalStatus.DRAFT;

    // ── Timestamps ───────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum ProposalStatus {
        DRAFT,
        SENT,
        VIEWED,
        SIGNED,
        DECLINED,
        EXPIRED,
        CONVERTED
    }
}
