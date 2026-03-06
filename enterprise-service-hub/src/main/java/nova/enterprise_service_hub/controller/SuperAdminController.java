package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.PlatformHealthDTO;
import nova.enterprise_service_hub.dto.TenantCreateRequest;
import nova.enterprise_service_hub.dto.TenantDTO;
import nova.enterprise_service_hub.dto.TenantPatchRequest;
import nova.enterprise_service_hub.model.SubscriptionPlan;
import nova.enterprise_service_hub.service.FeatureGateService;
import nova.enterprise_service_hub.service.TenantService;
import nova.enterprise_service_hub.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Super-Admin Controller — Platform-level tenant management.
 * <p>
 * <b>All endpoints require {@code ROLE_SUPER_ADMIN}.</b>
 * The tenant filter is bypassed for cross-tenant visibility
 * by clearing the {@link TenantContext} before each operation.
 */
@RestController
@RequestMapping("/v1/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final TenantService tenantService;
    private final FeatureGateService featureGateService;

    public SuperAdminController(TenantService tenantService, FeatureGateService featureGateService) {
        this.tenantService = tenantService;
        this.featureGateService = featureGateService;
    }

    // ── Tenant CRUD ──────────────────────────────────────────────────────

    @GetMapping("/tenants")
    public ResponseEntity<List<TenantDTO>> listTenants() {
        TenantContext.clear();
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @GetMapping("/tenants/{id}")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable Long id) {
        TenantContext.clear();
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    @GetMapping("/tenants/by-tenant-id/{tenantId}")
    public ResponseEntity<TenantDTO> getTenantByTenantId(@PathVariable String tenantId) {
        TenantContext.clear();
        return ResponseEntity.ok(tenantService.getTenantByTenantId(tenantId));
    }

    @PostMapping("/tenants")
    public ResponseEntity<TenantDTO> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        TenantContext.clear();
        TenantDTO created = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/tenants/{id}")
    public ResponseEntity<TenantDTO> updateTenant(
            @PathVariable Long id, @RequestBody TenantPatchRequest request) {
        TenantContext.clear();
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @PostMapping("/tenants/{id}/suspend")
    public ResponseEntity<Void> suspendTenant(@PathVariable Long id) {
        TenantContext.clear();
        tenantService.suspendTenant(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tenants/{id}/activate")
    public ResponseEntity<Void> activateTenant(@PathVariable Long id) {
        TenantContext.clear();
        tenantService.activateTenant(id);
        return ResponseEntity.noContent().build();
    }

    // ── Feature Gating Info ──────────────────────────────────────────────

    @GetMapping("/plans/{plan}/modules")
    public ResponseEntity<Set<String>> getModulesForPlan(@PathVariable SubscriptionPlan plan) {
        return ResponseEntity.ok(featureGateService.getAllowedModulesForPlan(plan));
    }

    // ── Platform Health ──────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<PlatformHealthDTO> getPlatformHealth() {
        TenantContext.clear();
        return ResponseEntity.ok(tenantService.getPlatformHealth());
    }
}
