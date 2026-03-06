package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Message entity — client-admin communication thread.
 * <p>
 * Phase 7.1: Client Portal
 */
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_tenant", columnList = "tenant_id"),
        @Index(name = "idx_message_client", columnList = "client_user_id"),
        @Index(name = "idx_message_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_user_id")
    private ClientUser clientUser;

    @Column(name = "sender_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SenderType senderType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum SenderType {
        CLIENT, ADMIN
    }
}
