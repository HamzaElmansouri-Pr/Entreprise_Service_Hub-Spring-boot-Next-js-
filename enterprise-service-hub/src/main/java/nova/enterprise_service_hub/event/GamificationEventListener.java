package nova.enterprise_service_hub.event;

import nova.enterprise_service_hub.service.SkillTreeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for project completion events and awards XP to technologies.
 * Runs asynchronously on the aiTaskExecutor thread pool.
 */
@Component
public class GamificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(GamificationEventListener.class);

    private final SkillTreeService skillTreeService;

    public GamificationEventListener(SkillTreeService skillTreeService) {
        this.skillTreeService = skillTreeService;
    }

    @Async("aiTaskExecutor")
    @EventListener
    public void handleProjectCompleted(ProjectCompletedEvent event) {
        log.info("Project completed event: project='{}', technologies={}, tenant={}",
                event.getProjectName(), event.getTechnologies(), event.getTenantId());

        try {
            skillTreeService.awardProjectXp(
                    event.getTenantId(),
                    event.getTechnologies(),
                    null // userId not available from project creation context; XP is tenant-wide
            );
        } catch (Exception e) {
            log.error("Failed to award XP for project '{}': {}",
                    event.getProjectName(), e.getMessage(), e);
        }
    }
}
