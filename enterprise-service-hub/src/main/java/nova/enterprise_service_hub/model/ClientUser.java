package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Client User entity — external client accounts for the Client Portal.
 * <p>
 * Phase 7.1: Client Portal
 * <p>
 * Clients are invited by tenant admins and can view their own
 * projects and invoices.
 */
@Entity
@Table(name = "client_users", indexes = {
        @Index(name = "idx_client_email", columnList = "email", unique = true),
        @Index(name = "idx_client_tenant", columnList = "tenant_id"),
        @Index(name = "idx_client_company", columnList = "company_name")
})
@Getter
@Setter
@NoArgsConstructor
public class ClientUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotBlank
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", length = 200)
    private String passwordHash;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "invite_token", length = 128)
    private String inviteToken;

    @Column(name = "invite_accepted")
    private boolean inviteAccepted = false;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
