package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import nova.enterprise_service_hub.security.TenantEntityListener;

import java.time.Instant;

@Entity
@Audited
@EntityListeners(TenantEntityListener.class)
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_sub_tenant", columnList = "tenant_id", unique = true),
        @Index(name = "idx_sub_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tenant ID is required")
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @NotBlank(message = "Plan name is required")
    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum SubscriptionStatus {
        ACTIVE,
        CANCELLED,
        EXPIRED
    }
}
