package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.GamificationDTO;
import nova.enterprise_service_hub.dto.GamificationDTO.*;
import nova.enterprise_service_hub.model.Badge;
import nova.enterprise_service_hub.model.SkillNode;
import nova.enterprise_service_hub.model.SkillNode.SkillCategory;
import nova.enterprise_service_hub.repository.BadgeRepository;
import nova.enterprise_service_hub.repository.SkillNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the Tech-Stack XP system and Skill Tree.
 * <p>
 * Awards XP when projects are completed, maintains skill levels,
 * provides the Career Roadmap (OCP Java SE 17 curriculum tree),
 * and triggers badge awards on milestones.
 */
@Service
@Transactional(readOnly = true)
public class SkillTreeService {

    private static final Logger log = LoggerFactory.getLogger(SkillTreeService.class);

    /** Base XP awarded per technology when a project is completed */
    public static final int PROJECT_COMPLETION_XP = 50;

    private final SkillNodeRepository skillNodeRepository;
    private final BadgeRepository badgeRepository;

    public SkillTreeService(SkillNodeRepository skillNodeRepository,
                            BadgeRepository badgeRepository) {
        this.skillNodeRepository = skillNodeRepository;
        this.badgeRepository = badgeRepository;
    }

    // ── XP Award ─────────────────────────────────────────────────────────

    /**
     * Award XP for each technology used in a completed project.
     */
    @Transactional
    public void awardProjectXp(String tenantId, List<String> technologies, Long userId) {
        if (technologies == null || technologies.isEmpty()) return;

        for (String tech : technologies) {
            String normalized = tech.trim();
            if (normalized.isEmpty()) continue;

            SkillNode node = skillNodeRepository
                    .findByTenantIdAndTechnologyName(tenantId, normalized)
                    .orElseGet(() -> {
                        SkillNode newNode = new SkillNode();
                        newNode.setTenantId(tenantId);
                        newNode.setTechnologyName(normalized);
                        newNode.setCategory(categorize(normalized));
                        return newNode;
                    });

            int oldLevel = node.getLevel();
            node.addXp(PROJECT_COMPLETION_XP);
            node.setProjectCount(node.getProjectCount() + 1);
            SkillNode saved = skillNodeRepository.save(node);

            // Check for level-up badge awards
            if (saved.getLevel() > oldLevel && userId != null) {
                checkLevelBadges(saved, userId, tenantId);
            }
        }

        // Check polyglot badge
        if (userId != null) {
            checkPolyglotBadge(userId, tenantId);
        }

        log.info("Awarded {}xp each to {} technologies for tenant {}",
                PROJECT_COMPLETION_XP, technologies.size(), tenantId);
    }

    // ── Skill Profile ────────────────────────────────────────────────────

    /**
     * Build the complete skill profile for a tenant.
     */
    public SkillProfile getSkillProfile(String tenantId) {
        List<SkillNode> skills = skillNodeRepository.findAllByTenantIdOrderByXpDesc(tenantId);
        int totalXp = skillNodeRepository.totalXpByTenant(tenantId);
        long mastered = skillNodeRepository.countByTenantIdAndLevelGreaterThanEqual(tenantId, 5);

        List<SkillNodeResponse> responses = skills.stream()
                .map(this::toResponse)
                .toList();

        Map<SkillCategory, List<SkillNode>> byCategory = skills.stream()
                .collect(Collectors.groupingBy(SkillNode::getCategory));

        List<CategorySummary> categories = byCategory.entrySet().stream()
                .map(e -> new CategorySummary(
                        e.getKey(),
                        formatCategoryLabel(e.getKey()),
                        e.getValue().stream().mapToInt(SkillNode::getXp).sum(),
                        e.getValue().size(),
                        (int) e.getValue().stream().mapToInt(SkillNode::getLevel).average().orElse(0)
                ))
                .sorted(Comparator.comparingInt(CategorySummary::totalXp).reversed())
                .toList();

        int overallLevel = totalXp > 0 ? Math.min(99, (totalXp / (200 * Math.max(1, skills.size()))) + 1) : 1;

        return new SkillProfile(totalXp, overallLevel, skills.size(), mastered, responses, categories);
    }

    // ── Career Roadmap (OCP Java SE 17 Curriculum) ───────────────────────

    /**
     * Build the OCP Java SE 17 Developer career roadmap with live progress.
     */
    public CareerRoadmap getCareerRoadmap(String tenantId) {
        List<RoadmapNode> nodes = buildOcpRoadmap(tenantId);
        long unlocked = nodes.stream().filter(RoadmapNode::unlocked).count();
        double overall = nodes.isEmpty() ? 0 : (unlocked * 100.0) / nodes.size();

        return new CareerRoadmap(
                "OCP Java SE 17 Developer",
                "Oracle Certified Professional: Java SE 17 Developer curriculum mapped to your real-world experience",
                nodes,
                nodes.size(),
                (int) unlocked,
                Math.round(overall * 10.0) / 10.0
        );
    }

    private List<RoadmapNode> buildOcpRoadmap(String tenantId) {
        List<RoadmapNode> nodes = new ArrayList<>();

        // Each node maps to OCP exam topics — XP is drawn from related technologies
        addRoadmapNode(nodes, tenantId, "java-fundamentals", null,
                "Java Fundamentals", "Data types, operators, control flow, String API",
                SkillCategory.JAVA_FUNDAMENTALS, 200);

        addRoadmapNode(nodes, tenantId, "oop-concepts", "java-fundamentals",
                "Object-Oriented Programming", "Classes, inheritance, polymorphism, interfaces, enums",
                SkillCategory.OOP, 300);

        addRoadmapNode(nodes, tenantId, "functional-prog", "oop-concepts",
                "Functional Programming", "Lambda expressions, functional interfaces, streams API",
                SkillCategory.FUNCTIONAL_PROGRAMMING, 400);

        addRoadmapNode(nodes, tenantId, "collections-generics", "oop-concepts",
                "Collections & Generics", "List, Set, Map, Queue, Deque, generics, Comparable/Comparator",
                SkillCategory.COLLECTIONS_GENERICS, 300);

        addRoadmapNode(nodes, tenantId, "concurrency", "functional-prog",
                "Concurrency & Multithreading", "Threads, ExecutorService, synchronized, concurrent collections",
                SkillCategory.CONCURRENCY, 500);

        addRoadmapNode(nodes, tenantId, "io-nio", "collections-generics",
                "I/O & NIO.2", "File I/O, serialization, Path, Files, Streams on files",
                SkillCategory.IO_NIO, 300);

        addRoadmapNode(nodes, tenantId, "jdbc-database", "io-nio",
                "JDBC & Database", "JDBC API, transactions, PreparedStatement, connection pooling",
                SkillCategory.JDBC_DATABASE, 400);

        // Extended tree: real tech categories
        addRoadmapNode(nodes, tenantId, "backend-frameworks", "jdbc-database",
                "Backend Frameworks", "Spring Boot, Quarkus, Micronaut — enterprise backend mastery",
                SkillCategory.BACKEND_FRAMEWORK, 500);

        addRoadmapNode(nodes, tenantId, "frontend-frameworks", "oop-concepts",
                "Frontend Frameworks", "React, Angular, Vue, Next.js — UI/UX engineering",
                SkillCategory.FRONTEND_FRAMEWORK, 400);

        addRoadmapNode(nodes, tenantId, "cloud-devops", "backend-frameworks",
                "Cloud & DevOps", "Docker, Kubernetes, CI/CD, AWS/GCP/Azure",
                SkillCategory.CLOUD_DEVOPS, 600);

        addRoadmapNode(nodes, tenantId, "security", "backend-frameworks",
                "Security", "OAuth2, JWT, Spring Security, OWASP, encryption",
                SkillCategory.SECURITY, 400);

        return nodes;
    }

    private void addRoadmapNode(List<RoadmapNode> nodes, String tenantId,
                                 String nodeId, String parentId,
                                 String title, String description,
                                 SkillCategory category, int requiredXp) {
        List<SkillNode> categorySkills = skillNodeRepository
                .findAllByTenantIdAndCategoryOrderByXpDesc(tenantId, category);

        int currentXp = categorySkills.stream().mapToInt(SkillNode::getXp).sum();
        boolean unlocked = currentXp >= requiredXp;
        double progress = requiredXp > 0 ? Math.min(100.0, (currentXp * 100.0) / requiredXp) : 0;

        nodes.add(new RoadmapNode(nodeId, parentId, title, description, category,
                requiredXp, currentXp, unlocked, Math.round(progress * 10.0) / 10.0));
    }

    // ── Technology → Category Mapping ────────────────────────────────────

    /**
     * Map a technology name to a skill category.
     * Uses pattern matching for well-known technologies.
     */
    public static SkillCategory categorize(String technology) {
        String lower = technology.toLowerCase();

        // Java fundamentals
        if (lower.matches("java|java se|jdk|jre|javac")) return SkillCategory.JAVA_FUNDAMENTALS;

        // OOP-related Java
        if (lower.matches("oop|design patterns|solid|uml")) return SkillCategory.OOP;

        // Functional programming
        if (lower.matches("rxjava|reactor|project reactor|streams|lambda")) return SkillCategory.FUNCTIONAL_PROGRAMMING;

        // Collections / generics
        if (lower.contains("collections") || lower.contains("generics")) return SkillCategory.COLLECTIONS_GENERICS;

        // Concurrency
        if (lower.matches(".*concurrent.*|.*thread.*|.*async.*|virtual threads")) return SkillCategory.CONCURRENCY;

        // I/O
        if (lower.matches(".*nio.*|file io|serialization")) return SkillCategory.IO_NIO;

        // Database
        if (lower.matches(".*jdbc.*|.*sql.*|postgresql|mysql|oracle db|mongodb|redis|hibernate|jpa|flyway"))
            return SkillCategory.JDBC_DATABASE;

        // Backend frameworks
        if (lower.matches("spring.*|quarkus|micronaut|jakarta ee|java ee|jax-rs|servlet"))
            return SkillCategory.BACKEND_FRAMEWORK;

        // Frontend frameworks
        if (lower.matches("react.*|angular.*|vue.*|next.*|nuxt.*|svelte.*|typescript|javascript|html|css|tailwind.*"))
            return SkillCategory.FRONTEND_FRAMEWORK;

        // Cloud / DevOps
        if (lower.matches("docker|kubernetes|k8s|aws|gcp|azure|terraform|jenkins|github actions|ci/cd|nginx|helm"))
            return SkillCategory.CLOUD_DEVOPS;

        // Mobile
        if (lower.matches("android|ios|flutter|react native|kotlin|swift"))
            return SkillCategory.MOBILE;

        // Data Science
        if (lower.matches("python|pandas|numpy|tensorflow|pytorch|ml|machine learning|ai|data science"))
            return SkillCategory.DATA_SCIENCE;

        // Security
        if (lower.matches("oauth.*|jwt|spring security|keycloak|owasp|encryption|ssl|tls"))
            return SkillCategory.SECURITY;

        return SkillCategory.OTHER;
    }

    // ── Badge Checks ─────────────────────────────────────────────────────

    private void checkLevelBadges(SkillNode node, Long userId, String tenantId) {
        int level = node.getLevel();
        String tech = node.getTechnologyName();

        if (level >= 2) awardBadgeIfNew(userId, tenantId, Badge.BadgeType.TECH_APPRENTICE,
                "Reached Apprentice level in " + tech, tech);
        if (level >= 3) awardBadgeIfNew(userId, tenantId, Badge.BadgeType.TECH_PRACTITIONER,
                "Reached Practitioner level in " + tech, tech);
        if (level >= 5) awardBadgeIfNew(userId, tenantId, Badge.BadgeType.TECH_EXPERT,
                "Reached Expert level in " + tech, tech);
        if (level >= 10) awardBadgeIfNew(userId, tenantId, Badge.BadgeType.TECH_MASTER,
                "Reached Master level in " + tech, tech);
    }

    private void checkPolyglotBadge(Long userId, String tenantId) {
        long practitionerCount = skillNodeRepository.countByTenantIdAndLevelGreaterThanEqual(tenantId, 3);
        if (practitionerCount >= 5) {
            awardBadgeIfNew(userId, tenantId, Badge.BadgeType.POLYGLOT,
                    "5+ technologies at Practitioner level or above", null);
        }
    }

    private void awardBadgeIfNew(Long userId, String tenantId, Badge.BadgeType type,
                                  String description, String metadata) {
        if (!badgeRepository.existsByUserIdAndBadgeType(userId, type)) {
            Badge badge = new Badge();
            badge.setUserId(userId);
            badge.setTenantId(tenantId);
            badge.setBadgeType(type);
            badge.setDescription(description);
            badge.setMetadata(metadata);
            badgeRepository.save(badge);
            log.info("Badge awarded: {} to user {} — {}", type, userId, description);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private SkillNodeResponse toResponse(SkillNode node) {
        return new SkillNodeResponse(
                node.getId(),
                node.getTechnologyName(),
                node.getCategory(),
                node.getXp(),
                node.getLevel(),
                node.xpInCurrentLevel(),
                node.xpToNextLevel(),
                node.getProjectCount(),
                GamificationDTO.tierForLevel(node.getLevel())
        );
    }

    private String formatCategoryLabel(SkillCategory category) {
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
