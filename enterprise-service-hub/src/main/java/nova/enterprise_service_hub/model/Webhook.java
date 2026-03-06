package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Webhook registration — allows tenants to receive HTTP callbacks for key events.
 * <p>
 * Phase 2.3: Webhook System
 */
@Entity
@Table(name = "webhooks", indexes = {
        @Index(name = "idx_webhook_tenant", columnList = "tenant_id"),
        @Index(name = "idx_webhook_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 255)
    private String secret;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "webhook_events", joinColumns = @JoinColumn(name = "webhook_id"))
    @Column(name = "event_type", length = 50)
    private Set<String> events = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 200)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
