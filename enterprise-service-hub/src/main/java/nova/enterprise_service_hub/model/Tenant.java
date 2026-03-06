package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a tenant (organisation / agency) on the platform.
 * <p>
 * Each tenant has a unique {@code tenantId} string that appears as
 * {@code tenant_id} on every tenant-aware entity.  The Super-Admin
 * manages tenants from the platform-level dashboard.
 */
@Entity
@Audited

@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenant_tenant_id", columnList = "tenant_id", unique = true),
        @Index(name = "idx_tenant_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Business name is required")
    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @NotBlank(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false, unique = true, length = 50)
    private String tenantId;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 30)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    /**
     * Set of enabled module keys for this tenant.
     * E.g. {@code ["ai_content", "finance", "advanced_analytics", "team_management"]}
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_modules", joinColumns = @JoinColumn(name = "tenant_id_fk"))
    @Column(name = "module_key", length = 50)
    private Set<String> enabledModules = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Tenant(String businessName, String tenantId, SubscriptionPlan plan) {
        this.businessName = businessName;
        this.tenantId = tenantId;
        this.subscriptionPlan = plan;
    }
}
