package nova.enterprise_service_hub.dto;

import org.hibernate.envers.RevisionType;
import java.time.Instant;

/**
 * DTO representing a single change (revision) to an audited entity.
 */
public record AuditLogDTO(
        Number revisionNumber,
        RevisionType revisionType,
        String entityType,
        Long entityId,
        Instant timestamp,
        Object entityState) {
}
