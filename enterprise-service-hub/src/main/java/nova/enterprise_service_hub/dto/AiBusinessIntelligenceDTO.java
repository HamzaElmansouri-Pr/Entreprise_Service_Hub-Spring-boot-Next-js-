package nova.enterprise_service_hub.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTOs for Agentic AI Business Intelligence.
 */
public final class AiBusinessIntelligenceDTO {

    private AiBusinessIntelligenceDTO() {}

    // ── Audit Report ─────────────────────────────────────────────────────

    /**
     * Result of the AI Autonomous Auditor scan.
     */
    public record AuditReport(
            String reportId,
            Instant generatedAt,
            String tenantId,
            int totalInvoicesScanned,
            int totalExpensesScanned,
            List<AuditAnomaly> anomalies,
            List<DuplicateGroup> duplicates,
            String summary,
            String status) {
    }

    public record AuditAnomaly(
            String type,
            String severity,
            String entity,
            Long entityId,
            String description,
            BigDecimal amount,
            String recommendation) {
    }

    public record DuplicateGroup(
            String vendor,
            BigDecimal amount,
            LocalDate date,
            List<Long> duplicateIds,
            String resolution) {
    }

    // ── Predictive Cash Flow ────────────────────────────────────────────

    /**
     * 90-day cash flow prediction.
     */
    public record CashFlowForecast(
            Instant generatedAt,
            String tenantId,
            BigDecimal currentBalance,
            BigDecimal predictedRevenue90d,
            BigDecimal predictedExpenses90d,
            BigDecimal predictedNetCashFlow,
            List<MonthlyForecast> monthlyBreakdown,
            List<String> riskFactors,
            String confidence) {
    }

    public record MonthlyForecast(
            String month,
            BigDecimal expectedRevenue,
            BigDecimal expectedExpenses,
            BigDecimal netCashFlow) {
    }

    // ── Conversational Query ────────────────────────────────────────────

    /**
     * Request payload for the SAP conversational interface.
     */
    public record ConversationalQuery(String question) {
    }

    /**
     * AI-generated answer grounded in real system data.
     */
    public record ConversationalAnswer(
            String question,
            String answer,
            List<DataPoint> supportingData,
            String confidence,
            Instant generatedAt) {
    }

    public record DataPoint(
            String label,
            String value,
            String source) {
    }

    // ── AI Report Event ─────────────────────────────────────────────────

    /**
     * Generic AI report status for async processing tracking.
     */
    public record AiReportStatus(
            String reportId,
            String type,
            String status,
            int progress,
            String message,
            Instant updatedAt) {
    }

    // ── Expense Category Breakdown ──────────────────────────────────────

    public record CategoryBreakdown(
            String category,
            BigDecimal total,
            double percentOfTotal) {
    }
}
