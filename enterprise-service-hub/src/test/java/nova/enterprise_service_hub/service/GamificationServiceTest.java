package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.GamificationDTO.*;
import nova.enterprise_service_hub.model.Badge;
import nova.enterprise_service_hub.model.SkillNode;
import nova.enterprise_service_hub.model.SkillNode.SkillCategory;
import nova.enterprise_service_hub.model.SprintSession;
import nova.enterprise_service_hub.repository.BadgeRepository;
import nova.enterprise_service_hub.repository.SkillNodeRepository;
import nova.enterprise_service_hub.repository.SprintSessionRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Skill-Tree Gamification module.
 * Tests XP awarding, level calculation, sprint lifecycle, and badge mechanics.
 */
@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock private SkillNodeRepository skillNodeRepo;
    @Mock private BadgeRepository badgeRepo;
    @Mock private SprintSessionRepository sprintRepo;

    @InjectMocks private SkillTreeService skillTreeService;
    @InjectMocks private SprintChallengeService sprintChallengeService;

    private static final String TENANT_ID = "test-tenant";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ══════════════════════════════════════════════════════════════════════
    // SkillNode: XP & Level Calculation
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SkillNode XP & Level")
    class SkillNodeXpLevel {

        @Test
        @DisplayName("new node starts at level 1 with 0 XP")
        void newNodeDefaults() {
            SkillNode node = new SkillNode();
            assertThat(node.getXp()).isZero();
            assertThat(node.getLevel()).isEqualTo(1);
            assertThat(node.xpInCurrentLevel()).isZero();
            assertThat(node.xpToNextLevel()).isEqualTo(200);
        }

        @Test
        @DisplayName("adding XP below threshold stays at level 1")
        void addXpBelowThreshold() {
            SkillNode node = new SkillNode();
            int level = node.addXp(150);
            assertThat(level).isEqualTo(1);
            assertThat(node.getXp()).isEqualTo(150);
            assertThat(node.xpInCurrentLevel()).isEqualTo(150);
            assertThat(node.xpToNextLevel()).isEqualTo(50);
        }

        @Test
        @DisplayName("adding 200 XP reaches level 2")
        void addXpReachesLevel2() {
            SkillNode node = new SkillNode();
            int level = node.addXp(200);
            assertThat(level).isEqualTo(2);
            assertThat(node.getLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("adding 1000 XP reaches level 6")
        void addLargeXp() {
            SkillNode node = new SkillNode();
            int level = node.addXp(1000);
            assertThat(level).isEqualTo(6); // floor(1000/200) + 1 = 6
        }

        @Test
        @DisplayName("level is capped at 99")
        void levelCapAt99() {
            SkillNode node = new SkillNode();
            int level = node.addXp(99999);
            assertThat(level).isEqualTo(99);
            assertThat(node.xpToNextLevel()).isZero();
        }

        @Test
        @DisplayName("XP accumulates across multiple additions")
        void xpAccumulates() {
            SkillNode node = new SkillNode();
            node.addXp(50);
            node.addXp(50);
            node.addXp(50);
            node.addXp(50);
            assertThat(node.getXp()).isEqualTo(200);
            assertThat(node.getLevel()).isEqualTo(2);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Technology → Category Mapping
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Technology Categorization")
    class TechCategorization {

        @Test
        @DisplayName("Spring Boot maps to BACKEND_FRAMEWORK")
        void springBoot() {
            assertThat(SkillTreeService.categorize("Spring Boot")).isEqualTo(SkillCategory.BACKEND_FRAMEWORK);
        }

        @Test
        @DisplayName("React maps to FRONTEND_FRAMEWORK")
        void react() {
            assertThat(SkillTreeService.categorize("React")).isEqualTo(SkillCategory.FRONTEND_FRAMEWORK);
        }

        @Test
        @DisplayName("PostgreSQL maps to JDBC_DATABASE")
        void postgresql() {
            assertThat(SkillTreeService.categorize("PostgreSQL")).isEqualTo(SkillCategory.JDBC_DATABASE);
        }

        @Test
        @DisplayName("Docker maps to CLOUD_DEVOPS")
        void docker() {
            assertThat(SkillTreeService.categorize("Docker")).isEqualTo(SkillCategory.CLOUD_DEVOPS);
        }

        @Test
        @DisplayName("unknown technology maps to OTHER")
        void unknownTech() {
            assertThat(SkillTreeService.categorize("Cobol")).isEqualTo(SkillCategory.OTHER);
        }

        @Test
        @DisplayName("Java maps to JAVA_FUNDAMENTALS")
        void java() {
            assertThat(SkillTreeService.categorize("Java")).isEqualTo(SkillCategory.JAVA_FUNDAMENTALS);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // XP Award Service
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("XP Award")
    class XpAward {

        @Test
        @DisplayName("awarding XP creates new skill nodes for unknown technologies")
        void awardsNewNodes() {
            when(skillNodeRepo.findByTenantIdAndTechnologyName(eq(TENANT_ID), anyString()))
                    .thenReturn(Optional.empty());
            when(skillNodeRepo.save(any(SkillNode.class))).thenAnswer(inv -> {
                SkillNode node = inv.getArgument(0);
                node.setId(1L);
                return node;
            });
            when(skillNodeRepo.countByTenantIdAndLevelGreaterThanEqual(eq(TENANT_ID), anyInt()))
                    .thenReturn(0L);

            skillTreeService.awardProjectXp(TENANT_ID, List.of("Spring Boot", "React"), USER_ID);

            verify(skillNodeRepo, times(2)).save(any(SkillNode.class));
        }

        @Test
        @DisplayName("awarding XP adds to existing skill nodes")
        void awardsExistingNodes() {
            SkillNode existing = new SkillNode();
            existing.setId(1L);
            existing.setTenantId(TENANT_ID);
            existing.setTechnologyName("Spring Boot");
            existing.setXp(100);
            existing.setLevel(1);
            existing.setProjectCount(2);
            existing.setCategory(SkillCategory.BACKEND_FRAMEWORK);

            when(skillNodeRepo.findByTenantIdAndTechnologyName(TENANT_ID, "Spring Boot"))
                    .thenReturn(Optional.of(existing));
            when(skillNodeRepo.save(any(SkillNode.class))).thenAnswer(inv -> inv.getArgument(0));
            when(skillNodeRepo.countByTenantIdAndLevelGreaterThanEqual(eq(TENANT_ID), anyInt()))
                    .thenReturn(0L);

            skillTreeService.awardProjectXp(TENANT_ID, List.of("Spring Boot"), USER_ID);

            ArgumentCaptor<SkillNode> captor = ArgumentCaptor.forClass(SkillNode.class);
            verify(skillNodeRepo).save(captor.capture());
            SkillNode saved = captor.getValue();
            assertThat(saved.getXp()).isEqualTo(150); // 100 + 50
            assertThat(saved.getProjectCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("empty technology list does nothing")
        void emptyTechList() {
            skillTreeService.awardProjectXp(TENANT_ID, List.of(), USER_ID);
            verify(skillNodeRepo, never()).save(any());
        }

        @Test
        @DisplayName("null technology list does nothing")
        void nullTechList() {
            skillTreeService.awardProjectXp(TENANT_ID, null, USER_ID);
            verify(skillNodeRepo, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Sprint Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Sprint Lifecycle")
    class SprintLifecycle {

        @Test
        @DisplayName("starting a sprint creates an IN_PROGRESS session")
        void startSprint() {
            when(sprintRepo.findByUserIdAndStatus(USER_ID, SprintSession.SprintStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());
            when(sprintRepo.save(any(SprintSession.class))).thenAnswer(inv -> {
                SprintSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            SprintResponse response = sprintChallengeService.startSprint(
                    USER_ID, TENANT_ID,
                    new StartSprintRequest("Build login page", 25));

            assertThat(response.status()).isEqualTo("IN_PROGRESS");
            assertThat(response.targetMinutes()).isEqualTo(25);
            assertThat(response.taskDescription()).isEqualTo("Build login page");
        }

        @Test
        @DisplayName("starting a new sprint abandons previous one")
        void startNewAbandonsPrevious() {
            SprintSession existing = new SprintSession();
            existing.setId(1L);
            existing.setUserId(USER_ID);
            existing.setStartedAt(Instant.now().minus(1, ChronoUnit.HOURS));
            existing.setStatus(SprintSession.SprintStatus.IN_PROGRESS);

            when(sprintRepo.findByUserIdAndStatus(USER_ID, SprintSession.SprintStatus.IN_PROGRESS))
                    .thenReturn(Optional.of(existing))
                    .thenReturn(Optional.empty());
            when(sprintRepo.save(any(SprintSession.class))).thenAnswer(inv -> {
                SprintSession s = inv.getArgument(0);
                if (s.getId() == null) s.setId(2L);
                return s;
            });

            sprintChallengeService.startSprint(USER_ID, TENANT_ID,
                    new StartSprintRequest("New task", 45));

            // Should save twice: once the abandoned, once the new
            verify(sprintRepo, times(2)).save(any(SprintSession.class));
            assertThat(existing.getStatus()).isEqualTo(SprintSession.SprintStatus.ABANDONED);
        }

        @Test
        @DisplayName("completing a sprint awards XP based on focus score")
        void completeSprint() {
            SprintSession session = new SprintSession();
            session.setId(1L);
            session.setUserId(USER_ID);
            session.setTenantId(TENANT_ID);
            session.setTargetMinutes(25);
            session.setStartedAt(Instant.now().minus(25, ChronoUnit.MINUTES));
            session.setStatus(SprintSession.SprintStatus.IN_PROGRESS);

            when(sprintRepo.findByUserIdAndStatus(USER_ID, SprintSession.SprintStatus.IN_PROGRESS))
                    .thenReturn(Optional.of(session));
            when(sprintRepo.save(any(SprintSession.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sprintRepo.countCompletedByUser(USER_ID)).thenReturn(1L);
            when(sprintRepo.countCompletedByUserSince(eq(USER_ID), any())).thenReturn(1L);
            when(sprintRepo.findRecentCompleted(eq(USER_ID), any())).thenReturn(List.of(session));
            when(badgeRepo.existsByUserIdAndBadgeType(eq(USER_ID), any())).thenReturn(false);
            when(badgeRepo.save(any(Badge.class))).thenAnswer(inv -> inv.getArgument(0));

            SprintResponse response = sprintChallengeService.completeSprint(USER_ID, TENANT_ID,
                    new CompleteSprintRequest(85));

            assertThat(response.status()).isEqualTo("COMPLETED");
            assertThat(response.focusScore()).isEqualTo(85);
            assertThat(response.xpAwarded()).isPositive();
        }

        @Test
        @DisplayName("completing sprint with no active sprint throws exception")
        void completeNoActiveSprint() {
            when(sprintRepo.findByUserIdAndStatus(USER_ID, SprintSession.SprintStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> sprintChallengeService.completeSprint(USER_ID, TENANT_ID,
                    new CompleteSprintRequest(80)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No active sprint");
        }

        @Test
        @DisplayName("target minutes is clamped to 1-120 range")
        void targetMinutesClamped() {
            when(sprintRepo.findByUserIdAndStatus(USER_ID, SprintSession.SprintStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());
            when(sprintRepo.save(any(SprintSession.class))).thenAnswer(inv -> {
                SprintSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            SprintResponse response = sprintChallengeService.startSprint(
                    USER_ID, TENANT_ID,
                    new StartSprintRequest("Task", 200)); // exceeds max 120

            assertThat(response.targetMinutes()).isEqualTo(120);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SprintSession XP Calculation
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Sprint XP Calculation")
    class SprintXpCalc {

        @Test
        @DisplayName("perfect focus (100) awards 150% of target minutes as XP")
        void perfectFocus() {
            SprintSession session = new SprintSession();
            session.setTargetMinutes(25);
            session.setStartedAt(Instant.now().minus(25, ChronoUnit.MINUTES));

            int xp = session.complete(100);

            // multiplier = 0.5 + 1.0 = 1.5 → 25 * 1.5 = 37.5 → 38
            assertThat(xp).isEqualTo(38);
            assertThat(session.getStatus()).isEqualTo(SprintSession.SprintStatus.COMPLETED);
        }

        @Test
        @DisplayName("zero focus awards 50% of target minutes as XP")
        void zeroFocus() {
            SprintSession session = new SprintSession();
            session.setTargetMinutes(25);
            session.setStartedAt(Instant.now().minus(25, ChronoUnit.MINUTES));

            int xp = session.complete(0);

            // multiplier = 0.5 + 0.0 = 0.5 → 25 * 0.5 = 12.5 → 13
            assertThat(xp).isEqualTo(13);
        }

        @Test
        @DisplayName("focus score is clamped to 0-100")
        void focusScoreClamped() {
            SprintSession session = new SprintSession();
            session.setTargetMinutes(25);
            session.setStartedAt(Instant.now().minus(25, ChronoUnit.MINUTES));

            session.complete(150); // exceeds 100
            assertThat(session.getFocusScore()).isEqualTo(100);

            session.setStatus(SprintSession.SprintStatus.IN_PROGRESS); // reset for re-test
            session.setStartedAt(Instant.now().minus(25, ChronoUnit.MINUTES));
            session.complete(-10); // below 0
            assertThat(session.getFocusScore()).isZero();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Badge Mechanics
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Badge Awards")
    class BadgeAwards {

        @Test
        @DisplayName("first sprint completion awards FIRST_SPRINT badge")
        void firstSprintBadge() {
            SprintSession session = new SprintSession();
            session.setId(1L);
            session.setUserId(USER_ID);
            session.setTenantId(TENANT_ID);
            session.setTargetMinutes(25);
            session.setStartedAt(Instant.now().minus(25, ChronoUnit.MINUTES));
            session.setStatus(SprintSession.SprintStatus.IN_PROGRESS);

            when(sprintRepo.findByUserIdAndStatus(USER_ID, SprintSession.SprintStatus.IN_PROGRESS))
                    .thenReturn(Optional.of(session));
            when(sprintRepo.save(any(SprintSession.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sprintRepo.countCompletedByUser(USER_ID)).thenReturn(1L); // first sprint
            when(sprintRepo.countCompletedByUserSince(eq(USER_ID), any())).thenReturn(1L);
            when(sprintRepo.findRecentCompleted(eq(USER_ID), any())).thenReturn(List.of(session));
            when(badgeRepo.existsByUserIdAndBadgeType(USER_ID, Badge.BadgeType.FIRST_SPRINT)).thenReturn(false);
            when(badgeRepo.save(any(Badge.class))).thenAnswer(inv -> inv.getArgument(0));

            sprintChallengeService.completeSprint(USER_ID, TENANT_ID, new CompleteSprintRequest(80));

            ArgumentCaptor<Badge> captor = ArgumentCaptor.forClass(Badge.class);
            verify(badgeRepo, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(Badge::getBadgeType)
                    .contains(Badge.BadgeType.FIRST_SPRINT);
        }

        @Test
        @DisplayName("deep focus badge awarded for 60-min sprint with 90+ focus")
        void deepFocusBadge() {
            SprintSession session = new SprintSession();
            session.setId(1L);
            session.setUserId(USER_ID);
            session.setTenantId(TENANT_ID);
            session.setTargetMinutes(60);
            session.setStartedAt(Instant.now().minus(60, ChronoUnit.MINUTES));
            session.setStatus(SprintSession.SprintStatus.IN_PROGRESS);

            when(sprintRepo.findByUserIdAndStatus(USER_ID, SprintSession.SprintStatus.IN_PROGRESS))
                    .thenReturn(Optional.of(session));
            when(sprintRepo.save(any(SprintSession.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sprintRepo.countCompletedByUser(USER_ID)).thenReturn(5L);
            when(sprintRepo.countCompletedByUserSince(eq(USER_ID), any())).thenReturn(1L);
            when(sprintRepo.findRecentCompleted(eq(USER_ID), any())).thenReturn(List.of(session));
            when(badgeRepo.existsByUserIdAndBadgeType(eq(USER_ID), any())).thenReturn(false);
            when(badgeRepo.save(any(Badge.class))).thenAnswer(inv -> inv.getArgument(0));

            sprintChallengeService.completeSprint(USER_ID, TENANT_ID, new CompleteSprintRequest(95));

            ArgumentCaptor<Badge> captor = ArgumentCaptor.forClass(Badge.class);
            verify(badgeRepo, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(Badge::getBadgeType)
                    .contains(Badge.BadgeType.DEEP_FOCUS);
        }

        @Test
        @DisplayName("existing badge is not re-awarded")
        void noDuplicateBadge() {
            SprintSession session = new SprintSession();
            session.setId(1L);
            session.setUserId(USER_ID);
            session.setTenantId(TENANT_ID);
            session.setTargetMinutes(25);
            session.setStartedAt(Instant.now().minus(25, ChronoUnit.MINUTES));
            session.setStatus(SprintSession.SprintStatus.IN_PROGRESS);

            when(sprintRepo.findByUserIdAndStatus(USER_ID, SprintSession.SprintStatus.IN_PROGRESS))
                    .thenReturn(Optional.of(session));
            when(sprintRepo.save(any(SprintSession.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sprintRepo.countCompletedByUser(USER_ID)).thenReturn(1L);
            when(sprintRepo.countCompletedByUserSince(eq(USER_ID), any())).thenReturn(1L);
            when(sprintRepo.findRecentCompleted(eq(USER_ID), any())).thenReturn(List.of(session));
            when(badgeRepo.existsByUserIdAndBadgeType(eq(USER_ID), any())).thenReturn(true);

            sprintChallengeService.completeSprint(USER_ID, TENANT_ID, new CompleteSprintRequest(80));

            verify(badgeRepo, never()).save(any(Badge.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // DTO Helpers
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DTO Helpers")
    class DtoHelpers {

        @Test
        @DisplayName("tierForLevel returns correct tier names")
        void tierMapping() {
            assertThat(nova.enterprise_service_hub.dto.GamificationDTO.tierForLevel(1)).isEqualTo("Novice");
            assertThat(nova.enterprise_service_hub.dto.GamificationDTO.tierForLevel(2)).isEqualTo("Apprentice");
            assertThat(nova.enterprise_service_hub.dto.GamificationDTO.tierForLevel(3)).isEqualTo("Practitioner");
            assertThat(nova.enterprise_service_hub.dto.GamificationDTO.tierForLevel(5)).isEqualTo("Expert");
            assertThat(nova.enterprise_service_hub.dto.GamificationDTO.tierForLevel(10)).isEqualTo("Master");
        }

        @Test
        @DisplayName("badgeLabel returns human-readable labels")
        void badgeLabels() {
            assertThat(nova.enterprise_service_hub.dto.GamificationDTO.badgeLabel(Badge.BadgeType.FIRST_SPRINT))
                    .isEqualTo("First Sprint");
            assertThat(nova.enterprise_service_hub.dto.GamificationDTO.badgeLabel(Badge.BadgeType.POLYGLOT))
                    .isEqualTo("Polyglot");
            assertThat(nova.enterprise_service_hub.dto.GamificationDTO.badgeLabel(Badge.BadgeType.FULL_STACK))
                    .isEqualTo("Full Stack");
        }
    }
}
