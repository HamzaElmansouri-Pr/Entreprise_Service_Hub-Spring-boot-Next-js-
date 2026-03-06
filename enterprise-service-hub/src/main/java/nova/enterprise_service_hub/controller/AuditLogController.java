package nova.enterprise_service_hub.controller;

import nova.enterprise_service_hub.dto.AuditLogDTO;
import nova.enterprise_service_hub.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for exposing Envers Audit logs to SUPER_ADMIN users.
 */
@RestController
@RequestMapping("/v1/audit")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogDTO>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(auditLogService.getEntityHistory(entityType, entityId));
    }
}
