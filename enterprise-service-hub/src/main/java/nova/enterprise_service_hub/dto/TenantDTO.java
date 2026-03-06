package nova.enterprise_service_hub.dto;

import nova.enterprise_service_hub.model.SubscriptionPlan;

import java.time.Instant;
import java.util.Set;

public record TenantDTO(
        Long id,
        String businessName,
        String tenantId,
        String contactEmail,
        SubscriptionPlan subscriptionPlan,
        Set<String> enabledModules,
        boolean active,
        long userCount,
        Instant createdAt,
        Instant updatedAt) {
}
