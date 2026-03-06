package nova.enterprise_service_hub.dto;

import nova.enterprise_service_hub.model.Badge;
import nova.enterprise_service_hub.model.SkillNode;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for the Skill-Tree Gamification module.
 */
public final class GamificationDTO {

    private GamificationDTO() {}

    // ── Skill Tree ──────────────────────────────────────────────────────

    /**
     * A single skill node in the leveling tree.
     */
    public record SkillNodeResponse(
            Long id,
            String technologyName,
            SkillNode.SkillCategory category,
            int xp,
            int level,
            int xpInCurrentLevel,
            int xpToNextLevel,
            int projectCount,
            String tier
    ) {}

    /**
     * Complete skill profile for a tenant.
     */
    public record SkillProfile(
            int totalXp,
            int overallLevel,
            long totalSkills,
            long masteredSkills,
            List<SkillNodeResponse> skills,
            List<CategorySummary> categories
    ) {}

    /**
     * Summary of skills grouped by category.
     */
    public record CategorySummary(
            SkillNode.SkillCategory category,
            String label,
            int totalXp,
            long skillCount,
            int avgLevel
    ) {}

    /**
     * XP tier label derived from level.
     */
    public static String tierForLevel(int level) {
        if (level >= 10) return "Master";
        if (level >= 5) return "Expert";
        if (level >= 3) return "Practitioner";
        if (level >= 2) return "Apprentice";
        return "Novice";
    }

    // ── Career Roadmap ──────────────────────────────────────────────────

    /**
     * A node in the OCP Java SE 17 curriculum tree.
     */
    public record RoadmapNode(
            String nodeId,
            String parentNodeId,
            String title,
            String description,
            SkillNode.SkillCategory category,
            int requiredXp,
            int currentXp,
            boolean unlocked,
            double progressPercent
    ) {}

    /**
     * Complete career roadmap with progress tracking.
     */
    public record CareerRoadmap(
            String title,
            String description,
            List<RoadmapNode> nodes,
            int totalNodes,
            int unlockedNodes,
            double overallProgress
    ) {}

    // ── Sprint Sessions ─────────────────────────────────────────────────

    /**
     * Request to start a new sprint.
     */
    public record StartSprintRequest(
            String taskDescription,
            int targetMinutes
    ) {}

    /**
     * Request to complete a sprint.
     */
    public record CompleteSprintRequest(
            int focusScore
    ) {}

    /**
     * Sprint session response.
     */
    public record SprintResponse(
            Long id,
            String taskDescription,
            int targetMinutes,
            Instant startedAt,
            Instant completedAt,
            Integer actualMinutes,
            String status,
            Integer focusScore,
            int xpAwarded
    ) {}

    /**
     * Sprint statistics for a user.
     */
    public record SprintStats(
            long totalCompleted,
            int totalFocusMinutes,
            double averageFocusScore,
            long sprintsToday,
            long sprintsThisWeek,
            int currentStreak
    ) {}

    // ── Badges ──────────────────────────────────────────────────────────

    /**
     * Badge response.
     */
    public record BadgeResponse(
            Long id,
            Badge.BadgeType badgeType,
            String label,
            String description,
            String icon,
            String metadata,
            Instant earnedAt
    ) {}

    /**
     * Get a human-readable label for a badge type.
     */
    public static String badgeLabel(Badge.BadgeType type) {
        return switch (type) {
            case FIRST_SPRINT -> "First Sprint";
            case SPRINT_STREAK_3 -> "Hat Trick";
            case SPRINT_STREAK_7 -> "Week Warrior";
            case SPRINT_STREAK_30 -> "Legendary Focus";
            case DEEP_FOCUS -> "Deep Focus";
            case TECH_APPRENTICE -> "Apprentice";
            case TECH_PRACTITIONER -> "Practitioner";
            case TECH_EXPERT -> "Expert";
            case TECH_MASTER -> "Master";
            case POLYGLOT -> "Polyglot";
            case FIRST_PROJECT -> "First Project";
            case PROJECT_VETERAN -> "Veteran";
            case PROJECT_LEGEND -> "Legend";
            case PORTFOLIO_LIVE -> "Portfolio Live";
            case FULL_STACK -> "Full Stack";
        };
    }

    /**
     * Get an icon name for a badge type (Lucide icon name).
     */
    public static String badgeIcon(Badge.BadgeType type) {
        return switch (type) {
            case FIRST_SPRINT, SPRINT_STREAK_3, SPRINT_STREAK_7, SPRINT_STREAK_30 -> "timer";
            case DEEP_FOCUS -> "brain";
            case TECH_APPRENTICE, TECH_PRACTITIONER -> "code";
            case TECH_EXPERT, TECH_MASTER -> "crown";
            case POLYGLOT -> "languages";
            case FIRST_PROJECT, PROJECT_VETERAN, PROJECT_LEGEND -> "folder-check";
            case PORTFOLIO_LIVE -> "globe";
            case FULL_STACK -> "layers";
        };
    }

    // ── Public Portfolio ─────────────────────────────────────────────────

    /**
     * Public-facing skills section for the visitor portfolio.
     */
    public record PublicSkillsProfile(
            List<PublicSkill> skills,
            List<CategorySummary> categories,
            int totalXp,
            int overallLevel,
            long projectsCompleted,
            List<String> topBadges
    ) {}

    /**
     * A single skill for public display (no internal IDs).
     */
    public record PublicSkill(
            String technologyName,
            String category,
            int level,
            String tier,
            int projectCount
    ) {}

    // ── Gamification Dashboard Summary ──────────────────────────────────

    /**
     * Combined gamification overview for the admin dashboard card.
     */
    public record GamificationSummary(
            int totalXp,
            int overallLevel,
            long totalSkills,
            long totalBadges,
            long sprintsCompleted,
            int focusMinutesThisWeek,
            String topTechnology
    ) {}
}
