package nova.enterprise_service_hub.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import nova.enterprise_service_hub.dto.AuditLogDTO;
import nova.enterprise_service_hub.model.AgencyService;
import nova.enterprise_service_hub.model.Project;
import nova.enterprise_service_hub.model.User;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Audit Log Service — Queries Hibernate Envers for entity revision history.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Map<String, Class<?>> AUDITED_ENTITIES = Map.of(
            "Project", Project.class,
            "AgencyService", AgencyService.class,
            "User", User.class,
            "Invoice", nova.enterprise_service_hub.model.Invoice.class,
            "Tenant", nova.enterprise_service_hub.model.Tenant.class);

    /**
     * Retrieves the revision history for a specific entity ID.
     */
    public List<AuditLogDTO> getEntityHistory(String entityType, Long entityId) {
        Class<?> clazz = AUDITED_ENTITIES.get(entityType);
        if (clazz == null) {
            throw new IllegalArgumentException("Unsupported auditable entity type: " + entityType);
        }

        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        // This returns Object[] where:
        // [0] = the entity instance at that revision
        // [1] = DefaultRevisionEntity (timestamp, id)
        // [2] = RevisionType (ADD, MOD, DEL)
        List<Object[]> queryResult = auditReader.createQuery()
                .forRevisionsOfEntity(clazz, false, true)
                .add(AuditEntity.id().eq(entityId))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        List<AuditLogDTO> history = new ArrayList<>();
        for (Object[] row : queryResult) {
            Object entityInst = row[0];
            org.hibernate.envers.DefaultRevisionEntity revEntity = (org.hibernate.envers.DefaultRevisionEntity) row[1];
            RevisionType revType = (RevisionType) row[2];

            history.add(new AuditLogDTO(
                    revEntity.getId(),
                    revType,
                    entityType,
                    entityId,
                    Instant.ofEpochMilli(revEntity.getTimestamp()),
                    entityInst));
        }

        return history;
    }
}
