package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.AiBusinessIntelligenceDTO.*;
import nova.enterprise_service_hub.event.AiReportEvent;
import nova.enterprise_service_hub.event.AiReportStore;
import nova.enterprise_service_hub.security.TenantContext;
import nova.enterprise_service_hub.service.ConversationalQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Agentic AI Business Intelligence.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>POST /v1/ai/bi/audit — trigger autonomous audit</li>
 *   <li>GET  /v1/ai/bi/audit/{reportId} — get audit result</li>
 *   <li>POST /v1/ai/bi/cash-flow — trigger 90-day forecast</li>
 *   <li>GET  /v1/ai/bi/cash-flow/{reportId} — get forecast result</li>
 *   <li>GET  /v1/ai/bi/reports/{reportId}/status — poll status</li>
 *   <li>GET  /v1/ai/bi/reports/{reportId}/stream — SSE real-time progress</li>
 *   <li>POST /v1/ai/bi/query — conversational "SAP" query</li>
 * </ul>
 * <p>
 * Async reports publish a {@link AiReportEvent} via Spring Events.
 * The listener runs the report on the aiTaskExecutor thread pool
 * and stores results in the {@link AiReportStore}.
 */
@RestController
@RequestMapping("/v1/ai/bi")
@PreAuthorize("hasRole('ADMIN')")
public class AiBusinessIntelligenceController {

    private static final Logger log = LoggerFactory.getLogger(AiBusinessIntelligenceController.class);

    private final ApplicationEventPublisher eventPublisher;
    private final AiReportStore reportStore;
    private final ConversationalQueryService conversationalQueryService;

    public AiBusinessIntelligenceController(ApplicationEventPublisher eventPublisher,
                                             AiReportStore reportStore,
                                             ConversationalQueryService conversationalQueryService) {
        this.eventPublisher = eventPublisher;
        this.reportStore = reportStore;
        this.conversationalQueryService = conversationalQueryService;
    }

    // ── Autonomous Audit ─────────────────────────────────────────────────

    /**
     * POST /v1/ai/bi/audit — Triggers an autonomous financial audit.
     * The audit runs asynchronously; returns a reportId immediately.
     */
    @PostMapping("/audit")
    public ResponseEntity<Map<String, String>> triggerAudit() {
        String tenantId = TenantContext.getTenantId();
        String reportId = UUID.randomUUID().toString();

        reportStore.updateStatus(reportId, "AUDIT", "QUEUED", 0, "Audit queued");

        AiReportEvent event = new AiReportEvent(this, reportId, tenantId,
                AiReportEvent.ReportType.AUDIT);
        eventPublisher.publishEvent(event);

        log.info("Audit report triggered: reportId={}, tenant={}", reportId, tenantId);

        return ResponseEntity.accepted().body(Map.of(
                "reportId", reportId,
                "status", "QUEUED",
                "message", "Audit started. Use the reportId to poll status or stream progress."));
    }

    /**
     * GET /v1/ai/bi/audit/{reportId} — Retrieves a completed audit report.
     */
    @GetMapping("/audit/{reportId}")
    public ResponseEntity<AuditReport> getAuditReport(@PathVariable String reportId) {
        AuditReport report = reportStore.getResult(reportId, AuditReport.class);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    // ── Predictive Cash-Flow ─────────────────────────────────────────────

    /**
     * POST /v1/ai/bi/cash-flow — Triggers a 90-day cash flow forecast.
     */
    @PostMapping("/cash-flow")
    public ResponseEntity<Map<String, String>> triggerCashFlowForecast() {
        String tenantId = TenantContext.getTenantId();
        String reportId = UUID.randomUUID().toString();

        reportStore.updateStatus(reportId, "CASH_FLOW", "QUEUED", 0, "Forecast queued");

        AiReportEvent event = new AiReportEvent(this, reportId, tenantId,
                AiReportEvent.ReportType.CASH_FLOW_FORECAST);
        eventPublisher.publishEvent(event);

        log.info("Cash flow forecast triggered: reportId={}, tenant={}", reportId, tenantId);

        return ResponseEntity.accepted().body(Map.of(
                "reportId", reportId,
                "status", "QUEUED",
                "message", "Cash flow forecast started. Use the reportId to poll status or stream progress."));
    }

    /**
     * GET /v1/ai/bi/cash-flow/{reportId} — Retrieves a completed cash flow forecast.
     */
    @GetMapping("/cash-flow/{reportId}")
    public ResponseEntity<CashFlowForecast> getCashFlowForecast(@PathVariable String reportId) {
        CashFlowForecast forecast = reportStore.getResult(reportId, CashFlowForecast.class);
        if (forecast == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(forecast);
    }

    // ── Report Status & Streaming ────────────────────────────────────────

    /**
     * GET /v1/ai/bi/reports/{reportId}/status — Poll the status of any report.
     */
    @GetMapping("/reports/{reportId}/status")
    public ResponseEntity<AiReportStatus> getReportStatus(@PathVariable String reportId) {
        AiReportStatus status = reportStore.getStatus(reportId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    /**
     * GET /v1/ai/bi/reports/{reportId}/stream — SSE stream of report progress.
     * <p>
     * Sends JSON events like:
     * {@code data: {"reportId":"...","status":"PROCESSING","progress":40,"message":"..."}}
     * <p>
     * Auto-completes when the report finishes or times out (5 minutes).
     */
    @GetMapping(value = "/reports/{reportId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReportProgress(@PathVariable String reportId) {
        SseEmitter emitter = reportStore.createEmitter(reportId);

        // Immediately send current status if available
        AiReportStatus current = reportStore.getStatus(reportId);
        if (current != null) {
            reportStore.broadcastToEmitter(reportId, current);
        }

        return emitter;
    }

    // ── Conversational "SAP" Interface ───────────────────────────────────

    /**
     * POST /v1/ai/bi/query — Answer a natural language business question.
     * <p>
     * Synchronous — typically completes in 2-5 seconds.
     * All responses are grounded in real database data.
     */
    @PostMapping("/query")
    public ResponseEntity<ConversationalAnswer> queryBusinessData(
            @Valid @RequestBody ConversationalQuery request) {
        String tenantId = TenantContext.getTenantId();

        log.info("Conversational query from tenant {}: '{}'", tenantId,
                request.question().length() > 80
                        ? request.question().substring(0, 80) + "…"
                        : request.question());

        ConversationalAnswer answer = conversationalQueryService.answerQuery(
                tenantId, request.question());

        return ResponseEntity.ok(answer);
    }
}
