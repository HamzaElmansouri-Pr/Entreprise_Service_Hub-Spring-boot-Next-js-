package nova.enterprise_service_hub.security;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JPA Entity Listener — Automatically stamps {@code tenant_id} on every
 * INSERT and UPDATE for entities that implement {@link TenantAware}.
 * <p>
 * Pulls the active tenant from {@link TenantContext} so service code
 * never needs to manually set it.
 * <p>
 * <b>Super-Admin bypass:</b> Users with {@code ROLE_SUPER_ADMIN} can
 * modify entities belonging to any tenant.
 */
public class TenantEntityListener {

    @PrePersist
    public void setTenantOnCreate(Object entity) {
        if (entity instanceof TenantAware tenantAware) {
            if (tenantAware.getTenantId() == null || tenantAware.getTenantId().isBlank()) {
                String currentTenant = TenantContext.getTenantId();
                if (currentTenant != null) {
                    tenantAware.setTenantId(currentTenant);
                }
            }
        }
    }

    @PreUpdate
    public void verifyTenantOnUpdate(Object entity) {
        if (isSuperAdmin()) {
            return; // Super-Admin can modify any tenant's entities
        }
        if (entity instanceof TenantAware tenantAware) {
            String currentTenant = TenantContext.getTenantId();
            if (currentTenant != null && tenantAware.getTenantId() != null
                    && !currentTenant.equals(tenantAware.getTenantId())) {
                throw new SecurityException(
                        "Tenant mismatch: cannot modify entity belonging to tenant '"
                                + tenantAware.getTenantId() + "' from tenant '" + currentTenant + "'");
            }
        }
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SUPER_ADMIN"::equals);
    }
}
