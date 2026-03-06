package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.GamificationDTO;
import nova.enterprise_service_hub.dto.GamificationDTO.*;
import nova.enterprise_service_hub.model.Badge;
import nova.enterprise_service_hub.model.SprintSession;
import nova.enterprise_service_hub.model.SprintSession.SprintStatus;
import nova.enterprise_service_hub.repository.BadgeRepository;
import nova.enterprise_service_hub.repository.SprintSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Manages Sprint Challenge / "Focus Mode" sessions.
 * <p>
 * Users start a timed sprint, work without distractions, then complete it.
 * Completing sprints awards XP bonuses and contributes to streak badges.
 */
@Service
@Transactional(readOnly = true)
public class SprintChallengeService {

    private static final Logger log = LoggerFactory.getLogger(SprintChallengeService.class);

    private final SprintSessionRepository sprintRepo;
    private final BadgeRepository badgeRepo;

    public SprintChallengeService(SprintSessionRepository sprintRepo,
                                   BadgeRepository badgeRepo) {
        this.sprintRepo = sprintRepo;
        this.badgeRepo = badgeRepo;
    }

    // ── Sprint Lifecycle ─────────────────────────────────────────────────

    /**
     * Start a new focus sprint. Only one active sprint per user at a time.
     */
    @Transactional
    public SprintResponse startSprint(Long userId, String tenantId, StartSprintRequest request) {
        // Check for existing active sprint
        sprintRepo.findByUserIdAndStatus(userId, SprintStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    existing.abandon();
                    sprintRepo.save(existing);
                    log.info("Abandoned previous sprint {} for user {}", existing.getId(), userId);
                });

        SprintSession session = new SprintSession();
        session.setUserId(userId);
        session.setTenantId(tenantId);
        session.setTaskDescription(request.taskDescription());
        session.setTargetMinutes(Math.max(1, Math.min(120, request.targetMinutes())));
        session.setStartedAt(Instant.now());
        session.setStatus(SprintStatus.IN_PROGRESS);

        SprintSession saved = sprintRepo.save(session);
        log.info("Sprint started: id={}, user={}, target={}min", saved.getId(), userId, saved.getTargetMinutes());
        return toResponse(saved);
    }

    /**
     * Complete the active sprint with a focus score.
     */
    @Transactional
    public SprintResponse completeSprint(Long userId, String tenantId, CompleteSprintRequest request) {
        SprintSession session = sprintRepo.findByUserIdAndStatus(userId, SprintStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("No active sprint found for user " + userId));

        int xp = session.complete(request.focusScore());
        SprintSession saved = sprintRepo.save(session);

        log.info("Sprint completed: id={}, user={}, focusScore={}, xpAwarded={}",
                saved.getId(), userId, saved.getFocusScore(), xp);

        // Check for badge awards
        checkSprintBadges(userId, tenantId, saved);

        return toResponse(saved);
    }

    /**
     * Abandon the current sprint.
     */
    @Transactional
    public SprintResponse abandonSprint(Long userId) {
        SprintSession session = sprintRepo.findByUserIdAndStatus(userId, SprintStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("No active sprint found for user " + userId));

        session.abandon();
        SprintSession saved = sprintRepo.save(session);
        log.info("Sprint abandoned: id={}, user={}", saved.getId(), userId);
        return toResponse(saved);
    }

    // ── Sprint Queries ───────────────────────────────────────────────────

    /**
     * Get the current active sprint for a user (if any).
     */
    public SprintResponse getActiveSprint(Long userId) {
        return sprintRepo.findByUserIdAndStatus(userId, SprintStatus.IN_PROGRESS)
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Get paginated sprint history for a user.
     */
    public Page<SprintResponse> getSprintHistory(Long userId, Pageable pageable) {
        return sprintRepo.findAllByUserIdOrderByStartedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get sprint statistics for a user.
     */
    public SprintStats getSprintStats(Long userId) {
        long totalCompleted = sprintRepo.countCompletedByUser(userId);
        int totalMinutes = sprintRepo.totalFocusMinutesByUser(userId);
        double avgScore = sprintRepo.averageFocusScoreByUser(userId);

        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekStart = todayStart.minus(7, ChronoUnit.DAYS);

        long sprintsToday = sprintRepo.countCompletedByUserSince(userId, todayStart);
        long sprintsThisWeek = sprintRepo.countCompletedByUserSince(userId, weekStart);

        int currentStreak = calculateStreak(userId);

        return new SprintStats(
                totalCompleted,
                totalMinutes,
                Math.round(avgScore * 10.0) / 10.0,
                sprintsToday,
                sprintsThisWeek,
                currentStreak
        );
    }

    // ── Streak Calculation ───────────────────────────────────────────────

    /**
     * Calculate current consecutive-day streak of completed sprints.
     */
    private int calculateStreak(Long userId) {
        Instant thirtyDaysAgo = Instant.now().minus(31, ChronoUnit.DAYS);
        List<SprintSession> recent = sprintRepo.findRecentCompleted(userId, thirtyDaysAgo);

        if (recent.isEmpty()) return 0;

        int streak = 0;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Check if there's a sprint today
        boolean hasToday = recent.stream()
                .anyMatch(s -> s.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(today));

        // Allow checking from yesterday if no sprint today (streak isn't broken until end of day)
        final LocalDate startDate = hasToday ? today : today.minusDays(1);

        for (int i = 0; i < 31; i++) {
            LocalDate day = startDate.minusDays(i);
            boolean hasDay = recent.stream()
                    .anyMatch(s -> s.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(day));
            if (hasDay) {
                streak++;
            } else {
                break;
            }
        }

        return streak;
    }

    // ── Badge Checks ─────────────────────────────────────────────────────

    private void checkSprintBadges(Long userId, String tenantId, SprintSession session) {
        long completed = sprintRepo.countCompletedByUser(userId);

        // First sprint
        if (completed == 1) {
            awardBadgeIfNew(userId, tenantId, Badge.BadgeType.FIRST_SPRINT,
                    "Completed your first focus sprint!", null);
        }

        // 3 sprints in one day
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long sprintsToday = sprintRepo.countCompletedByUserSince(userId, todayStart);
        if (sprintsToday >= 3) {
            awardBadgeIfNew(userId, tenantId, Badge.BadgeType.SPRINT_STREAK_3,
                    "Completed 3 sprints in one day — Hat Trick!", null);
        }

        // Day streaks
        int streak = calculateStreak(userId);
        if (streak >= 7) {
            awardBadgeIfNew(userId, tenantId, Badge.BadgeType.SPRINT_STREAK_7,
                    "7-day sprint streak — Week Warrior!", String.valueOf(streak));
        }
        if (streak >= 30) {
            awardBadgeIfNew(userId, tenantId, Badge.BadgeType.SPRINT_STREAK_30,
                    "30-day sprint streak — Legendary focus!", String.valueOf(streak));
        }

        // Deep focus: 60+ min sprint with 90+ focus score
        if (session.getTargetMinutes() >= 60 && session.getFocusScore() != null && session.getFocusScore() >= 90) {
            awardBadgeIfNew(userId, tenantId, Badge.BadgeType.DEEP_FOCUS,
                    "Completed a 60-minute sprint with 90+ focus score", null);
        }
    }

    private void awardBadgeIfNew(Long userId, String tenantId, Badge.BadgeType type,
                                  String description, String metadata) {
        if (!badgeRepo.existsByUserIdAndBadgeType(userId, type)) {
            Badge badge = new Badge();
            badge.setUserId(userId);
            badge.setTenantId(tenantId);
            badge.setBadgeType(type);
            badge.setDescription(description);
            badge.setMetadata(metadata);
            badgeRepo.save(badge);
            log.info("Badge awarded: {} to user {} — {}", type, userId, description);
        }
    }

    // ── Mapping ──────────────────────────────────────────────────────────

    private SprintResponse toResponse(SprintSession s) {
        return new SprintResponse(
                s.getId(),
                s.getTaskDescription(),
                s.getTargetMinutes(),
                s.getStartedAt(),
                s.getCompletedAt(),
                s.getActualMinutes(),
                s.getStatus().name(),
                s.getFocusScore(),
                s.getXpAwarded()
        );
    }
}
