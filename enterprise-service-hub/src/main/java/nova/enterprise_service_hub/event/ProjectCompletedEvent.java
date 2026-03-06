package nova.enterprise_service_hub.event;

import org.springframework.context.ApplicationEvent;

/**
 * Published when a project is created or updated with technologies.
 * Triggers XP award to each technology in the project.
 */
public class ProjectCompletedEvent extends ApplicationEvent {

    private final String tenantId;
    private final Long projectId;
    private final String projectName;
    private final java.util.List<String> technologies;

    public ProjectCompletedEvent(Object source, String tenantId, Long projectId,
                                  String projectName, java.util.List<String> technologies) {
        super(source);
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.projectName = projectName;
        this.technologies = technologies;
    }

    public String getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public java.util.List<String> getTechnologies() { return technologies; }
}
