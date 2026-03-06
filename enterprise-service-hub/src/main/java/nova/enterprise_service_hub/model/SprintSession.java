package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nova.enterprise_service_hub.security.TenantAware;
import nova.enterprise_service_hub.security.TenantEntityListener;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.Instant;

/**
 * Tracks a focused development sprint ("Focus Mode").
 * <p>
 * Users start a sprint with a target duration, then mark it complete.
 * Completed sprints earn XP bonuses and contribute toward streak badges.
 */
@Entity
@EntityListeners({ AuditingEntityListener.class, TenantEntityListener.class })
@Table(name = "sprint_sessions", indexes = {
        @Index(name = "idx_sprint_tenant", columnList = "tenant_id"),
        @Index(name = "idx_sprint_user", columnList = "user_id"),
        @Index(name = "idx_sprint_started", columnList = "started_at")
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class SprintSession implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** What the user is working on during this sprint */
    @Size(max = 200)
    @Column(name = "task_description", length = 200)
    private String taskDescription;

    /** Target duration chosen by user (in minutes): 25, 45, 60 */
    @Min(1)
    @Column(name = "target_minutes", nullable = false)
    private int targetMinutes;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Actual duration in minutes once completed */
    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SprintStatus status = SprintStatus.IN_PROGRESS;

    /**
     * Focus quality score 0-100.  100 = completed full duration with no interruptions.
     * Client-side tracks tab-switches / idle — sent on completion.
     */
    @Min(0)
    @Column(name = "focus_score")
    private Integer focusScore;

    /** XP bonus awarded for completing this sprint */
    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ── Lifecycle ────────────────────────────────────────────────────────

    /**
     * Mark the sprint as completed. Returns computed XP bonus.
     */
    public int complete(int focusScore) {
        this.completedAt = Instant.now();
        this.actualMinutes = (int) Duration.between(startedAt, completedAt).toMinutes();
        this.focusScore = Math.min(100, Math.max(0, focusScore));
        this.status = SprintStatus.COMPLETED;

        // Base XP = target minutes.  Focus multiplier: score/100 applied as 0.5x – 1.5x
        double multiplier = 0.5 + (this.focusScore / 100.0);
        this.xpAwarded = (int) Math.round(targetMinutes * multiplier);
        return this.xpAwarded;
    }

    public void abandon() {
        this.completedAt = Instant.now();
        this.actualMinutes = (int) Duration.between(startedAt, completedAt).toMinutes();
        this.status = SprintStatus.ABANDONED;
    }

    // ── Enums ────────────────────────────────────────────────────────────

    public enum SprintStatus {
        IN_PROGRESS,
        COMPLETED,
        ABANDONED
    }
}
