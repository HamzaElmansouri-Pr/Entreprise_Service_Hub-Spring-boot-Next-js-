package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * API Key entity — enables B2B integrations alongside JWT auth.
 * <p>
 * Phase 6: API Key Authentication
 */
@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_key_hash", columnList = "key_hash", unique = true),
        @Index(name = "idx_api_key_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotBlank
    @Column(name = "key_hash", nullable = false, unique = true, length = 128)
    private String keyHash;

    @Column(name = "key_prefix", length = 10)
    private String keyPrefix;

    @Column(length = 200)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
