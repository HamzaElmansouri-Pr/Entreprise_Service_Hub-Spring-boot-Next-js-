package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.model.SubscriptionPlan;
import nova.enterprise_service_hub.model.Tenant;
import nova.enterprise_service_hub.repository.TenantRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Feature Gate Service — Enforces tier-based module access.
 * <p>
 * Each {@link SubscriptionPlan} grants access to a curated set of modules.
 * A module is available only if:
 * <ol>
 *   <li>The tenant's plan includes it, <b>AND</b></li>
 *   <li>The module is explicitly enabled in the tenant's {@code enabledModules} set</li>
 * </ol>
 */
@Service
public class FeatureGateService {

    /** Maps each plan to the full set of modules it unlocks. */
    private static final Map<SubscriptionPlan, Set<String>> PLAN_MODULES = Map.of(
            SubscriptionPlan.FREE, Set.of(),
            SubscriptionPlan.STARTER, Set.of("finance"),
            SubscriptionPlan.PROFESSIONAL, Set.of("finance", "ai_content", "advanced_analytics"),
            SubscriptionPlan.ELITE, Set.of("finance", "ai_content", "advanced_analytics", "team_management")
    );

    private final TenantRepository tenantRepository;

    public FeatureGateService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Check whether the current tenant (from {@link TenantContext}) has
     * access to the given module.
     *
     * @param moduleKey e.g. {@code "ai_content"}, {@code "finance"}
     * @return true if the tenant's plan allows it AND the module is enabled
     */
    public boolean isModuleAvailable(String moduleKey) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) return false;
        return isModuleAvailableForTenant(tenantId, moduleKey);
    }

    /**
     * Check whether a specific tenant has access to the given module.
     */
    public boolean isModuleAvailableForTenant(String tenantId, String moduleKey) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        if (tenant == null || !tenant.isActive()) return false;

        Set<String> planAllowed = PLAN_MODULES.getOrDefault(tenant.getSubscriptionPlan(), Set.of());
        return planAllowed.contains(moduleKey) && tenant.getEnabledModules().contains(moduleKey);
    }

    /**
     * Return all modules that the tenant's plan allows (regardless of
     * whether they are currently enabled).
     */
    public Set<String> getAllowedModulesForPlan(SubscriptionPlan plan) {
        return PLAN_MODULES.getOrDefault(plan, Set.of());
    }

    /**
     * Throws {@link SecurityException} if the current tenant cannot use the module.
     */
    public void requireModule(String moduleKey) {
        if (!isModuleAvailable(moduleKey)) {
            throw new SecurityException(
                    "Module '" + moduleKey + "' is not available for your subscription plan. "
                            + "Please upgrade your plan or contact your administrator.");
        }
    }
}
