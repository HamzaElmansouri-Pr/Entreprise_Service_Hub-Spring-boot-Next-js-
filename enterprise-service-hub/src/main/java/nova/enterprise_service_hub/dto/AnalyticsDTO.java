package nova.enterprise_service_hub.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Analytics response DTOs — Phase 2.5
 */
public class AnalyticsDTO {

    public record RevenueAnalytics(
            BigDecimal totalRevenue,
            BigDecimal paidRevenue,
            BigDecimal pendingRevenue,
            BigDecimal overdueRevenue,
            List<MonthlyRevenue> monthlyBreakdown
    ) {}

    public record MonthlyRevenue(
            String month,
            BigDecimal amount,
            long invoiceCount
    ) {}

    public record ProjectAnalytics(
            long totalProjects,
            long activeProjects,
            long archivedProjects,
            double completionRate,
            Map<String, Long> projectsByTechnology
    ) {}

    public record UserAnalytics(
            long totalUsers,
            long activeUsers,
            Map<String, Long> usersByRole
    ) {}

    public record DashboardSummary(
            RevenueAnalytics revenue,
            ProjectAnalytics projects,
            UserAnalytics users,
            long totalServices,
            long activeServices
    ) {}

    public record InvoiceSummary(
            long total,
            long pending,
            long paid,
            long overdue,
            long cancelled
    ) {}
}
