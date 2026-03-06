package nova.enterprise_service_hub.event;

import nova.enterprise_service_hub.dto.AiBusinessIntelligenceDTO.AiReportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store + SSE broadcaster for AI report results and progress.
 * <p>
 * Frontend clients subscribe via SSE (/v1/ai/reports/{reportId}/stream)
 * and receive real-time updates as the AI agent processes data.
 */
@Component
public class AiReportStore {

    private static final Logger log = LoggerFactory.getLogger(AiReportStore.class);

    /** Completed/in-progress report results indexed by reportId */
    private final Map<String, Object> reportResults = new ConcurrentHashMap<>();

    /** Report status tracker */
    private final Map<String, AiReportStatus> reportStatuses = new ConcurrentHashMap<>();

    /** Active SSE emitters for live progress */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // ── Status Management ────────────────────────────────────────────────

    public void updateStatus(String reportId, String type, String status, int progress, String message) {
        AiReportStatus reportStatus = new AiReportStatus(
                reportId, type, status, progress, message, Instant.now());
        reportStatuses.put(reportId, reportStatus);
        broadcastToEmitter(reportId, reportStatus);
    }

    public AiReportStatus getStatus(String reportId) {
        return reportStatuses.get(reportId);
    }

    // ── Result Storage ───────────────────────────────────────────────────

    public void storeResult(String reportId, Object result) {
        reportResults.put(reportId, result);
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult(String reportId, Class<T> type) {
        Object result = reportResults.get(reportId);
        if (result != null && type.isInstance(result)) {
            return (T) result;
        }
        return null;
    }

    // ── SSE Emitter Management ───────────────────────────────────────────

    public SseEmitter createEmitter(String reportId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout
        emitter.onCompletion(() -> emitters.remove(reportId));
        emitter.onTimeout(() -> emitters.remove(reportId));
        emitter.onError(e -> emitters.remove(reportId));
        emitters.put(reportId, emitter);
        return emitter;
    }

    public void broadcastToEmitter(String reportId, AiReportStatus status) {
        SseEmitter emitter = emitters.get(reportId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("report-status")
                        .data(status));

                if ("COMPLETED".equals(status.status()) || "FAILED".equals(status.status())) {
                    emitter.complete();
                    emitters.remove(reportId);
                }
            } catch (IOException e) {
                log.warn("Failed to send SSE for report {}: {}", reportId, e.getMessage());
                emitters.remove(reportId);
            }
        }
    }

    /** Evict reports older than 1 hour (called by scheduled cleanup) */
    public void evictOldReports() {
        Instant cutoff = Instant.now().minusSeconds(3600);
        reportStatuses.entrySet().removeIf(e -> e.getValue().updatedAt().isBefore(cutoff));
        reportResults.entrySet().removeIf(e -> {
            AiReportStatus status = reportStatuses.get(e.getKey());
            return status == null;
        });
    }
}
