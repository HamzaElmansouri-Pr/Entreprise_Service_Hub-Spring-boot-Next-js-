package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.model.SubscriptionPlan;
import nova.enterprise_service_hub.model.Tenant;
import nova.enterprise_service_hub.repository.TenantRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FeatureGateService — validates tier-based module access.
 */
@ExtendWith(MockitoExtension.class)
class FeatureGateServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private FeatureGateService featureGateService;

    private Tenant createTenant(SubscriptionPlan plan, Set<String> enabledModules) {
        Tenant t = new Tenant("Test Corp", "tenant-test", plan);
        t.setId(1L);
        t.setActive(true);
        t.setEnabledModules(enabledModules);
        return t;
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("FREE plan should have no modules available")
    void freePlanNoModules() {
        Tenant tenant = createTenant(SubscriptionPlan.FREE, Set.of("finance"));
        when(tenantRepository.findByTenantId("tenant-test")).thenReturn(Optional.of(tenant));

        boolean result = featureGateService.isModuleAvailableForTenant("tenant-test", "finance");

        assertThat(result).isFalse(); // FREE plan doesn't include finance
    }

    @Test
    @DisplayName("STARTER plan should have finance module")
    void starterPlanHasFinance() {
        Tenant tenant = createTenant(SubscriptionPlan.STARTER, Set.of("finance"));
        when(tenantRepository.findByTenantId("tenant-test")).thenReturn(Optional.of(tenant));

        boolean result = featureGateService.isModuleAvailableForTenant("tenant-test", "finance");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Module must be both plan-allowed AND explicitly enabled")
    void moduleRequiresBothPlanAndEnabled() {
        // PROFESSIONAL allows ai_content, but tenant hasn't enabled it
        Tenant tenant = createTenant(SubscriptionPlan.PROFESSIONAL, Set.of("finance"));
        when(tenantRepository.findByTenantId("tenant-test")).thenReturn(Optional.of(tenant));

        boolean result = featureGateService.isModuleAvailableForTenant("tenant-test", "ai_content");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("ELITE plan should have all modules")
    void elitePlanHasAll() {
        Set<String> allModules = Set.of("finance", "ai_content", "advanced_analytics", "team_management");
        Tenant tenant = createTenant(SubscriptionPlan.ELITE, allModules);
        when(tenantRepository.findByTenantId("tenant-test")).thenReturn(Optional.of(tenant));

        for (String module : allModules) {
            assertThat(featureGateService.isModuleAvailableForTenant("tenant-test", module))
                    .as("Module %s should be available for ELITE", module)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Inactive tenant should have no module access")
    void inactiveTenantNoAccess() {
        Tenant tenant = createTenant(SubscriptionPlan.ELITE, Set.of("finance"));
        tenant.setActive(false);
        when(tenantRepository.findByTenantId("tenant-test")).thenReturn(Optional.of(tenant));

        boolean result = featureGateService.isModuleAvailableForTenant("tenant-test", "finance");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Non-existent tenant should have no module access")
    void unknownTenantNoAccess() {
        when(tenantRepository.findByTenantId("unknown")).thenReturn(Optional.empty());

        boolean result = featureGateService.isModuleAvailableForTenant("unknown", "finance");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("requireModule should throw SecurityException for unavailable module")
    void requireModuleThrows() {
        TenantContext.setTenantId("tenant-test");
        Tenant tenant = createTenant(SubscriptionPlan.FREE, Set.of());
        when(tenantRepository.findByTenantId("tenant-test")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> featureGateService.requireModule("finance"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("getAllowedModulesForPlan returns correct set")
    void getAllowedModules() {
        Set<String> profModules = featureGateService.getAllowedModulesForPlan(SubscriptionPlan.PROFESSIONAL);

        assertThat(profModules).containsExactlyInAnyOrder("finance", "ai_content", "advanced_analytics");
    }
}
