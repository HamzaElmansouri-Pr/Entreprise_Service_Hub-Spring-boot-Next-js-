package nova.enterprise_service_hub.security;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA Entity Listener — Automatically stamps {@code tenant_id} on every
 * INSERT and UPDATE for entities that implement {@link TenantAware}.
 * <p>
 * Pulls the active tenant from {@link TenantContext} so service code
 * never needs to manually set it.
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
}
