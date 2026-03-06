package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
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

import java.time.Instant;

/**
 * A badge earned by a user for gamification achievements.
 * <p>
 * Badges are awarded automatically by the system when milestones are reached:
 * sprint streaks, technology mastery, project completions, etc.
 */
@Entity
@EntityListeners({ AuditingEntityListener.class, TenantEntityListener.class })
@Table(name = "badges", indexes = {
        @Index(name = "idx_badge_tenant", columnList = "tenant_id"),
        @Index(name = "idx_badge_user", columnList = "user_id"),
        @Index(name = "idx_badge_type", columnList = "badge_type"),
        @Index(name = "idx_badge_user_type", columnList = "user_id, badge_type")
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class Badge implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", length = 40, nullable = false)
    private BadgeType badgeType;

    /** Human-readable description of how this badge was earned */
    @Size(max = 200)
    @Column(length = 200)
    private String description;

    /** Optional metadata: technology name, sprint count, project name, etc. */
    @Size(max = 100)
    @Column(length = 100)
    private String metadata;

    @CreatedDate
    @Column(name = "earned_at", updatable = false, nullable = false)
    private Instant earnedAt;

    // ── Badge Types ──────────────────────────────────────────────────────

    public enum BadgeType {
        // Sprint milestones
        FIRST_SPRINT,        // Complete first sprint
        SPRINT_STREAK_3,     // 3 consecutive sprints in one day
        SPRINT_STREAK_7,     // 7 consecutive day streak
        SPRINT_STREAK_30,    // 30-day streak — legendary
        DEEP_FOCUS,          // Complete a 60-min sprint with 90+ focus score

        // Technology mastery
        TECH_APPRENTICE,     // Reach level 2 in any technology
        TECH_PRACTITIONER,   // Reach level 3
        TECH_EXPERT,         // Reach level 5
        TECH_MASTER,         // Reach level 10
        POLYGLOT,            // Have 5+ technologies at level 3+

        // Project milestones
        FIRST_PROJECT,       // Complete first project
        PROJECT_VETERAN,     // Complete 10 projects
        PROJECT_LEGEND,      // Complete 25 projects

        // Portfolio
        PORTFOLIO_LIVE,      // Skills visible on public portfolio
        FULL_STACK            // Have skills in both BACKEND_FRAMEWORK and FRONTEND_FRAMEWORK categories at level 3+
    }
}
