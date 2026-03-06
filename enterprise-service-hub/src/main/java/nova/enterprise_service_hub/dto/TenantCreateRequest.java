package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import nova.enterprise_service_hub.model.SubscriptionPlan;

import java.util.Set;

public record TenantCreateRequest(
        @NotBlank(message = "Business name is required") String businessName,
        @Email(message = "Must be a valid email") String contactEmail,
        SubscriptionPlan subscriptionPlan,
        Set<String> enabledModules) {
}
