package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nova.enterprise_service_hub.security.TenantAware;
import nova.enterprise_service_hub.security.TenantEntityListener;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;

/**
 * In-app notification entity.
 * <p>
 * Used to notify users of events (project created, invoice overdue, etc.).
 * Supports real-time push via SSE (Server-Sent Events).
 */
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user", columnList = "user_id"),
        @Index(name = "idx_notif_tenant", columnList = "tenant_id"),
        @Index(name = "idx_notif_read", columnList = "is_read"),
        @Index(name = "idx_notif_created", columnList = "created_at")
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class Notification implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "user_id")
    private Long userId;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type = NotificationType.INFO;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum NotificationType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    public Notification(String tenantId, Long userId, String title, String message, NotificationType type) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
    }
}
