package nova.enterprise_service_hub.dto;

import java.util.List;
import java.util.Map;

/**
 * Platform-wide health and metrics visible only to Super-Admins.
 */
public record PlatformHealthDTO(
                long totalTenants,
                long activeTenants,
                long totalUsers,
                long activeUsers,
                long totalProjects,
                long totalServices,
                Map<String, Long> tenantsByPlan,
                List<TenantSummary> tenantSummaries,
                SystemMetrics systemMetrics,
                List<String> slowTenants) {

        public record TenantSummary(
                        String tenantId,
                        String businessName,
                        String plan,
                        long userCount,
                        long projectCount,
                        long serviceCount,
                        boolean active) {
        }

        public record SystemMetrics(
                        String jvmMemoryUsed,
                        String jvmMemoryMax,
                        double memoryUsagePercent,
                        int availableProcessors,
                        String javaVersion,
                        String uptime) {
        }
}
