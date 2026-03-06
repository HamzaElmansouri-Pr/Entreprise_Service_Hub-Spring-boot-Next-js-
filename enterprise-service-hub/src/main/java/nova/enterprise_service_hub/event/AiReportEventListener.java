package nova.enterprise_service_hub.event;

import nova.enterprise_service_hub.service.AiAuditService;
import nova.enterprise_service_hub.service.PredictiveCashFlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronous listener for AI report generation events.
 * <p>
 * All AI processing runs on the {@code aiTaskExecutor} thread pool,
 * ensuring the main request threads are never blocked by LLM calls
 * or heavy data aggregation.
 */
@Component
public class AiReportEventListener {

    private static final Logger log = LoggerFactory.getLogger(AiReportEventListener.class);

    private final AiAuditService auditService;
    private final PredictiveCashFlowService cashFlowService;
    private final AiReportStore reportStore;

    public AiReportEventListener(AiAuditService auditService,
                                  PredictiveCashFlowService cashFlowService,
                                  AiReportStore reportStore) {
        this.auditService = auditService;
        this.cashFlowService = cashFlowService;
        this.reportStore = reportStore;
    }

    @Async("aiTaskExecutor")
    @EventListener
    public void handleAiReportEvent(AiReportEvent event) {
        log.info("AI Report Event received: type={}, reportId={}, tenant={}",
                event.getType(), event.getReportId(), event.getTenantId());

        try {
            switch (event.getType()) {
                case AUDIT -> {
                    reportStore.updateStatus(event.getReportId(), "AUDIT", "PROCESSING", 10,
                            "Scanning invoices and expenses...");
                    var report = auditService.runAudit(event.getTenantId(), event.getReportId());
                    reportStore.storeResult(event.getReportId(), report);
                    reportStore.updateStatus(event.getReportId(), "AUDIT", "COMPLETED", 100,
                            "Audit complete: " + report.anomalies().size() + " anomalies found");
                }
                case CASH_FLOW_FORECAST -> {
                    reportStore.updateStatus(event.getReportId(), "CASH_FLOW", "PROCESSING", 10,
                            "Analyzing historical revenue data...");
                    var forecast = cashFlowService.generateForecast(event.getTenantId(), event.getReportId());
                    reportStore.storeResult(event.getReportId(), forecast);
                    reportStore.updateStatus(event.getReportId(), "CASH_FLOW", "COMPLETED", 100,
                            "90-day forecast generated");
                }
                default -> log.warn("Unhandled AI report type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("AI Report generation failed for {}: {}", event.getReportId(), e.getMessage(), e);
            reportStore.updateStatus(event.getReportId(), event.getType().name(), "FAILED", 0,
                    "Error: " + e.getMessage());
        }
    }
}
