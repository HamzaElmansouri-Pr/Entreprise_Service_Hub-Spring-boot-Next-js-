package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.TenantCreateRequest;
import nova.enterprise_service_hub.dto.TenantDTO;
import nova.enterprise_service_hub.dto.TenantPatchRequest;
import nova.enterprise_service_hub.model.SubscriptionPlan;
import nova.enterprise_service_hub.model.Tenant;
import nova.enterprise_service_hub.repository.AgencyServiceRepository;
import nova.enterprise_service_hub.repository.ProjectRepository;
import nova.enterprise_service_hub.repository.TenantRepository;
import nova.enterprise_service_hub.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantService — validates multi-tenancy isolation.
 */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private AgencyServiceRepository serviceRepository;
    @Spy  private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(
                tenantRepository, userRepository, projectRepository, serviceRepository, meterRegistry);
    }

    private Tenant createTestTenant(Long id, String tenantId, String name) {
        Tenant t = new Tenant(name, tenantId, SubscriptionPlan.STARTER);
        t.setId(id);
        t.setContactEmail(name.toLowerCase().replace(" ", "") + "@test.com");
        t.setActive(true);
        t.setEnabledModules(Set.of("finance"));
        return t;
    }

    @Nested
    @DisplayName("getAllTenants")
    class GetAllTenants {

        @Test
        @DisplayName("should return all tenants as DTOs")
        void returnsAllTenants() {
            Tenant t1 = createTestTenant(1L, "tenant-1", "Alpha Corp");
            Tenant t2 = createTestTenant(2L, "tenant-2", "Beta Inc");
            when(tenantRepository.findAllOrderByCreatedAtDesc()).thenReturn(List.of(t1, t2));
            when(userRepository.countByTenantId(anyString())).thenReturn(5L);

            List<TenantDTO> result = tenantService.getAllTenants();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).businessName()).isEqualTo("Alpha Corp");
            assertThat(result.get(1).businessName()).isEqualTo("Beta Inc");
        }

        @Test
        @DisplayName("should return empty list when no tenants exist")
        void returnsEmptyList() {
            when(tenantRepository.findAllOrderByCreatedAtDesc()).thenReturn(List.of());

            List<TenantDTO> result = tenantService.getAllTenants();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createTenant")
    class CreateTenant {

        @Test
        @DisplayName("should create tenant with generated UUID tenantId")
        void createsTenantWithDefaultPlan() {
            TenantCreateRequest request = new TenantCreateRequest(
                    "New Corp", "new@corp.com", null, null);

            when(tenantRepository.existsByTenantId(anyString())).thenReturn(false);
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
                Tenant saved = invocation.getArgument(0);
                saved.setId(99L);
                return saved;
            });
            when(userRepository.countByTenantId(anyString())).thenReturn(0L);

            TenantDTO result = tenantService.createTenant(request);

            assertThat(result.businessName()).isEqualTo("New Corp");
            assertThat(result.subscriptionPlan()).isEqualTo(SubscriptionPlan.FREE);
            assertThat(result.tenantId()).isNotBlank();
            verify(tenantRepository).save(any(Tenant.class));
        }
    }

    @Nested
    @DisplayName("suspendTenant / activateTenant")
    class TenantLifecycle {

        @Test
        @DisplayName("should suspend an active tenant")
        void suspendsTenant() {
            Tenant tenant = createTestTenant(1L, "t-1", "Corp");
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

            tenantService.suspendTenant(1L);

            assertThat(tenant.isActive()).isFalse();
            verify(tenantRepository).save(tenant);
        }

        @Test
        @DisplayName("should activate a suspended tenant")
        void activatesTenant() {
            Tenant tenant = createTestTenant(1L, "t-1", "Corp");
            tenant.setActive(false);
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

            tenantService.activateTenant(1L);

            assertThat(tenant.isActive()).isTrue();
            verify(tenantRepository).save(tenant);
        }

        @Test
        @DisplayName("should throw when tenant not found")
        void throwsWhenNotFound() {
            when(tenantRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.suspendTenant(999L))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("updateTenant")
    class UpdateTenant {

        @Test
        @DisplayName("should patch only provided fields")
        void patchesOnlyProvidedFields() {
            Tenant tenant = createTestTenant(1L, "t-1", "Old Name");
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
            when(tenantRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(userRepository.countByTenantId(anyString())).thenReturn(3L);

            TenantPatchRequest patch = new TenantPatchRequest(
                    "New Name", null, null, null, null);

            TenantDTO result = tenantService.updateTenant(1L, patch);

            assertThat(result.businessName()).isEqualTo("New Name");
            // email should remain unchanged
            assertThat(tenant.getContactEmail()).isEqualTo("oldname@test.com");
        }
    }
}
