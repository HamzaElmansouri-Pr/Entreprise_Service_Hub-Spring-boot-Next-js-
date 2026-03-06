package nova.enterprise_service_hub.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect — Automatically enables the Hibernate tenant filter
 * on every Spring Data JPA repository call when a tenant is active.
 * <p>
 * This ensures that <strong>every</strong> SELECT query executed through
 * repositories appends {@code WHERE tenant_id = :tenantId}, making it
 * impossible for "Client A" to access "Client B" data.
 * <p>
 * <b>Super-Admin bypass:</b> Users with {@code ROLE_SUPER_ADMIN} operate
 * cross-tenant and are exempt from the filter.
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Intercepts all repository method invocations and enables the
     * Hibernate {@code tenantFilter} if a tenant is present in the
     * current {@link TenantContext} — unless the caller is a Super-Admin.
     */
    @Before("execution(* nova.enterprise_service_hub.repository.*.*(..))")
    public void enableTenantFilter() {
        // Super-Admin users bypass tenant filtering entirely
        if (isSuperAdmin()) {
            return;
        }

        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter")
                    .setParameter("tenantId", tenantId);
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
