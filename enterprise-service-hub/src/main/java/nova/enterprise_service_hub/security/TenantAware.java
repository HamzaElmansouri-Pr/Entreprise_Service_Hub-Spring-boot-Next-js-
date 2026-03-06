package nova.enterprise_service_hub.security;

/**
 * Contract for entities that belong to a specific tenant.
 * <p>
 * Implemented by all core domain entities to enable automatic
 * tenant filtering via Hibernate {@code @Filter} and auto-population
 * via {@link TenantEntityListener}.
 */
public interface TenantAware {

    String getTenantId();

    void setTenantId(String tenantId);
}
