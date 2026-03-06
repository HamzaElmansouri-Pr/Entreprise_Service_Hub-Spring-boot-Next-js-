package nova.enterprise_service_hub.event;

import org.springframework.context.ApplicationEvent;

/**
 * Base event for all AI report generation tasks.
 * <p>
 * Published via Spring's {@link org.springframework.context.ApplicationEventPublisher}
 * and processed asynchronously by listeners on the {@code aiTaskExecutor} pool.
 * This ensures AI reports never block the main request-handling threads.
 */
public class AiReportEvent extends ApplicationEvent {

    private final String reportId;
    private final String tenantId;
    private final ReportType type;

    public AiReportEvent(Object source, String reportId, String tenantId, ReportType type) {
        super(source);
        this.reportId = reportId;
        this.tenantId = tenantId;
        this.type = type;
    }

    public String getReportId() { return reportId; }
    public String getTenantId() { return tenantId; }
    public ReportType getType() { return type; }

    public enum ReportType {
        AUDIT,
        CASH_FLOW_FORECAST,
        CONVERSATIONAL_QUERY
    }
}
