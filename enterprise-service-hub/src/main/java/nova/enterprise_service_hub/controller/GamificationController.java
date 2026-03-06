package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.GamificationDTO.*;
import nova.enterprise_service_hub.model.Badge;
import nova.enterprise_service_hub.model.User;
import nova.enterprise_service_hub.repository.BadgeRepository;
import nova.enterprise_service_hub.security.TenantContext;
import nova.enterprise_service_hub.service.PortfolioSyncService;
import nova.enterprise_service_hub.service.SkillTreeService;
import nova.enterprise_service_hub.service.SprintChallengeService;
import nova.enterprise_service_hub.dto.GamificationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Skill-Tree Gamification module.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>GET  /v1/gamification/skills          — Skill profile (all XP nodes)</li>
 *   <li>GET  /v1/gamification/roadmap          — Career roadmap (OCP tree + progress)</li>
 *   <li>POST /v1/gamification/sprints/start    — Start a focus sprint</li>
 *   <li>POST /v1/gamification/sprints/complete — Complete active sprint</li>
 *   <li>POST /v1/gamification/sprints/abandon  — Abandon active sprint</li>
 *   <li>GET  /v1/gamification/sprints/active   — Get active sprint</li>
 *   <li>GET  /v1/gamification/sprints/history  — Sprint history (paged)</li>
 *   <li>GET  /v1/gamification/sprints/stats    — Sprint statistics</li>
 *   <li>GET  /v1/gamification/badges           — All earned badges</li>
 *   <li>GET  /v1/gamification/summary          — Dashboard summary</li>
 *   <li>GET  /v1/gamification/public/{tenantId}— Public skills portfolio (unauthenticated)</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/gamification")
public class GamificationController {

    private final SkillTreeService skillTreeService;
    private final SprintChallengeService sprintService;
    private final PortfolioSyncService portfolioService;
    private final BadgeRepository badgeRepository;

    public GamificationController(SkillTreeService skillTreeService,
                                   SprintChallengeService sprintService,
                                   PortfolioSyncService portfolioService,
                                   BadgeRepository badgeRepository) {
        this.skillTreeService = skillTreeService;
        this.sprintService = sprintService;
        this.portfolioService = portfolioService;
        this.badgeRepository = badgeRepository;
    }

    // ── Skill Tree ──────────────────────────────────────────────────────

    @GetMapping("/skills")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillProfile> getSkillProfile() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(skillTreeService.getSkillProfile(tenantId));
    }

    @GetMapping("/roadmap")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CareerRoadmap> getCareerRoadmap() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(skillTreeService.getCareerRoadmap(tenantId));
    }

    // ── Sprint Challenges ───────────────────────────────────────────────

    @PostMapping("/sprints/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SprintResponse> startSprint(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody StartSprintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sprintService.startSprint(user.getId(), user.getTenantId(), request));
    }

    @PostMapping("/sprints/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SprintResponse> completeSprint(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CompleteSprintRequest request) {
        return ResponseEntity.ok(sprintService.completeSprint(user.getId(), user.getTenantId(), request));
    }

    @PostMapping("/sprints/abandon")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SprintResponse> abandonSprint(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(sprintService.abandonSprint(user.getId()));
    }

    @GetMapping("/sprints/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SprintResponse> getActiveSprint(@AuthenticationPrincipal User user) {
        SprintResponse active = sprintService.getActiveSprint(user.getId());
        return active != null ? ResponseEntity.ok(active) : ResponseEntity.noContent().build();
    }

    @GetMapping("/sprints/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SprintResponse>> getSprintHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(sprintService.getSprintHistory(user.getId(), pageable));
    }

    @GetMapping("/sprints/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SprintStats> getSprintStats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(sprintService.getSprintStats(user.getId()));
    }

    // ── Badges ──────────────────────────────────────────────────────────

    @GetMapping("/badges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BadgeResponse>> getBadges(@AuthenticationPrincipal User user) {
        List<Badge> badges = badgeRepository.findAllByUserIdOrderByEarnedAtDesc(user.getId());
        List<BadgeResponse> responses = badges.stream()
                .map(b -> new BadgeResponse(
                        b.getId(),
                        b.getBadgeType(),
                        GamificationDTO.badgeLabel(b.getBadgeType()),
                        b.getDescription(),
                        GamificationDTO.badgeIcon(b.getBadgeType()),
                        b.getMetadata(),
                        b.getEarnedAt()
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }

    // ── Dashboard Summary ───────────────────────────────────────────────

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GamificationSummary> getDashboardSummary() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(portfolioService.getDashboardSummary(tenantId));
    }

    // ── Public Portfolio (Unauthenticated) ──────────────────────────────

    @GetMapping("/public/{tenantId}")
    public ResponseEntity<PublicSkillsProfile> getPublicSkills(@PathVariable String tenantId) {
        return ResponseEntity.ok(portfolioService.getPublicProfile(tenantId));
    }
}
