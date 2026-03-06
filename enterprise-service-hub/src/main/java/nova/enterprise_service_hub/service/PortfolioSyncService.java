package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.GamificationDTO;
import nova.enterprise_service_hub.dto.GamificationDTO.*;
import nova.enterprise_service_hub.model.Badge;
import nova.enterprise_service_hub.model.SkillNode;
import nova.enterprise_service_hub.repository.BadgeRepository;
import nova.enterprise_service_hub.repository.SkillNodeRepository;
import nova.enterprise_service_hub.repository.SprintSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the public-facing skills portfolio from real project achievements.
 * <p>
 * This service aggregates skill data for the visitor-facing website,
 * ensuring the "Skills" section auto-updates from actual project completions
 * rather than requiring manual curation.
 */
@Service
@Transactional(readOnly = true)
public class PortfolioSyncService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioSyncService.class);

    private final SkillNodeRepository skillNodeRepo;
    private final BadgeRepository badgeRepo;
    private final SprintSessionRepository sprintRepo;

    public PortfolioSyncService(SkillNodeRepository skillNodeRepo,
                                 BadgeRepository badgeRepo,
                                 SprintSessionRepository sprintRepo) {
        this.skillNodeRepo = skillNodeRepo;
        this.badgeRepo = badgeRepo;
        this.sprintRepo = sprintRepo;
    }

    /**
     * Build a public-facing skills profile for a tenant.
     * This is exposed through a public (unauthenticated) endpoint.
     */
    public PublicSkillsProfile getPublicProfile(String tenantId) {
        List<SkillNode> skills = skillNodeRepo.findAllByTenantIdOrderByXpDesc(tenantId);
        int totalXp = skillNodeRepo.totalXpByTenant(tenantId);

        // Filter to skills with meaningful XP (level >= 2) to avoid noise
        List<PublicSkill> publicSkills = skills.stream()
                .filter(s -> s.getLevel() >= 2)
                .map(s -> new PublicSkill(
                        s.getTechnologyName(),
                        formatCategoryLabel(s.getCategory()),
                        s.getLevel(),
                        GamificationDTO.tierForLevel(s.getLevel()),
                        s.getProjectCount()
                ))
                .toList();

        // Category summaries
        var categories = skills.stream()
                .filter(s -> s.getLevel() >= 2)
                .collect(Collectors.groupingBy(SkillNode::getCategory))
                .entrySet().stream()
                .map(e -> new CategorySummary(
                        e.getKey(),
                        formatCategoryLabel(e.getKey()),
                        e.getValue().stream().mapToInt(SkillNode::getXp).sum(),
                        e.getValue().size(),
                        (int) e.getValue().stream().mapToInt(SkillNode::getLevel).average().orElse(0)
                ))
                .sorted((a, b) -> Integer.compare(b.totalXp(), a.totalXp()))
                .toList();

        // Total projects completed (sum of all project counts / rough unique count)
        long projectsCompleted = skills.stream()
                .mapToInt(SkillNode::getProjectCount)
                .max()
                .orElse(0);

        // Top badge labels
        List<Badge> badges = badgeRepo.findAllByTenantIdOrderByEarnedAtDesc(tenantId);
        List<String> topBadges = badges.stream()
                .limit(5)
                .map(b -> GamificationDTO.badgeLabel(b.getBadgeType()))
                .toList();

        int overallLevel = totalXp > 0 ? Math.min(99, (totalXp / (200 * Math.max(1, skills.size()))) + 1) : 1;

        return new PublicSkillsProfile(publicSkills, categories, totalXp, overallLevel, projectsCompleted, topBadges);
    }

    /**
     * Get a gamification summary for the admin dashboard.
     */
    public GamificationSummary getDashboardSummary(String tenantId) {
        List<SkillNode> skills = skillNodeRepo.findAllByTenantIdOrderByXpDesc(tenantId);
        int totalXp = skillNodeRepo.totalXpByTenant(tenantId);
        long totalBadges = badgeRepo.countByTenantId(tenantId);

        String topTech = skills.isEmpty() ? "—" : skills.getFirst().getTechnologyName();
        int overallLevel = totalXp > 0 ? Math.min(99, (totalXp / (200 * Math.max(1, skills.size()))) + 1) : 1;

        // Approximate sprint/focus data across all users in tenant
        // For now, use total badge count as a rough indicator
        return new GamificationSummary(
                totalXp,
                overallLevel,
                skills.size(),
                totalBadges,
                0, // Will be enriched when a tenant-level sprint query is added
                0,
                topTech
        );
    }

    private String formatCategoryLabel(SkillNode.SkillCategory category) {
        return switch (category) {
            case JAVA_FUNDAMENTALS -> "Java Fundamentals";
            case OOP -> "Object-Oriented Programming";
            case FUNCTIONAL_PROGRAMMING -> "Functional Programming";
            case COLLECTIONS_GENERICS -> "Collections & Generics";
            case CONCURRENCY -> "Concurrency";
            case IO_NIO -> "I/O & NIO.2";
            case JDBC_DATABASE -> "JDBC & Database";
            case BACKEND_FRAMEWORK -> "Backend Frameworks";
            case FRONTEND_FRAMEWORK -> "Frontend Frameworks";
            case CLOUD_DEVOPS -> "Cloud & DevOps";
            case MOBILE -> "Mobile";
            case DATA_SCIENCE -> "Data Science";
            case SECURITY -> "Security";
            case OTHER -> "Other";
        };
    }
}
