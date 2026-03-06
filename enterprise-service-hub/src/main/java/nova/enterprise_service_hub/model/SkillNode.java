package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Tracks cumulative experience points (XP) for a specific technology
 * within a tenant's context.
 * <p>
 * XP is awarded when projects/services using that technology are completed.
 * Level is derived from XP: level = floor(xp / 200) + 1, capped at 99.
 */
@Entity
@EntityListeners({ AuditingEntityListener.class, TenantEntityListener.class })
@Table(name = "skill_nodes", indexes = {
        @Index(name = "idx_skill_tenant", columnList = "tenant_id"),
        @Index(name = "idx_skill_tech", columnList = "technology_name"),
        @Index(name = "idx_skill_tenant_tech", columnList = "tenant_id, technology_name", unique = true)
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class SkillNode implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    /** Canonical technology name: "Spring Boot", "React", "PostgreSQL", etc. */
    @NotBlank
    @Size(max = 80)
    @Column(name = "technology_name", nullable = false, length = 80)
    private String technologyName;

    /** Cumulative experience points */
    @Min(0)
    @Column(nullable = false)
    private int xp = 0;

    /** Derived level: floor(xp / 200) + 1, capped at 99 */
    @Min(1)
    @Column(nullable = false)
    private int level = 1;

    /**
     * Category for grouping in the skill tree.
     * Mapped from the OCP Java SE 17 curriculum chapters + extended tech categories.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32, nullable = false)
    private SkillCategory category = SkillCategory.OTHER;

    /** Number of projects that contributed XP to this skill */
    @Column(name = "project_count", nullable = false)
    private int projectCount = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ── Level Calculation ────────────────────────────────────────────────

    /** XP required per level */
    public static final int XP_PER_LEVEL = 200;
    public static final int MAX_LEVEL = 99;

    /**
     * Add XP and recompute level. Returns the new level.
     */
    public int addXp(int amount) {
        this.xp += amount;
        this.level = Math.min(MAX_LEVEL, (this.xp / XP_PER_LEVEL) + 1);
        return this.level;
    }

    /**
     * XP progress within the current level (0–199).
     */
    public int xpInCurrentLevel() {
        return this.xp % XP_PER_LEVEL;
    }

    /**
     * XP remaining to reach the next level.
     */
    public int xpToNextLevel() {
        if (this.level >= MAX_LEVEL) return 0;
        return XP_PER_LEVEL - xpInCurrentLevel();
    }

    // ── Skill Categories ─────────────────────────────────────────────────

    public enum SkillCategory {
        // OCP Java SE 17 curriculum chapters
        JAVA_FUNDAMENTALS,
        OOP,
        FUNCTIONAL_PROGRAMMING,
        COLLECTIONS_GENERICS,
        CONCURRENCY,
        IO_NIO,
        JDBC_DATABASE,

        // Extended tech categories
        BACKEND_FRAMEWORK,
        FRONTEND_FRAMEWORK,
        CLOUD_DEVOPS,
        MOBILE,
        DATA_SCIENCE,
        SECURITY,
        OTHER
    }
}
