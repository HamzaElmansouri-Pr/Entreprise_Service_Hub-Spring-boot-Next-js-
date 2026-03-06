package nova.enterprise_service_hub.dto;

import nova.enterprise_service_hub.model.SubscriptionPlan;

import java.util.Set;

public record TenantPatchRequest(
        String businessName,
        String contactEmail,
        SubscriptionPlan subscriptionPlan,
        Set<String> enabledModules,
        Boolean active) {
}
