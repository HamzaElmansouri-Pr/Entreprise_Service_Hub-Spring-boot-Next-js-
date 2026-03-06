package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.AgencyServiceDTO;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.dto.ServicePatchRequest;
import nova.enterprise_service_hub.dto.ServiceRequest;
import nova.enterprise_service_hub.service.AgencyServiceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Agency Services.
 * <p>
 * GET endpoints are public (SEO + lead generation).
 * POST/PUT/PATCH/DELETE are restricted to ROLE_ADMIN.
 * PATCH enables selective field updates for the Backoffice.
 */
@RestController
@RequestMapping("/v1/services")
public class AgencyServiceController {

    private final AgencyServiceService serviceService;

    public AgencyServiceController(AgencyServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping
    public ResponseEntity<List<AgencyServiceDTO>> getAllActiveServices() {
        return ResponseEntity.ok(serviceService.findAllActive());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<AgencyServiceDTO>> getServicesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "displayOrder,asc") String sort) {
        String[] sortParts = sort.split(",");
        Sort sortObj = Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "asc"), sortParts[0]);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sortObj);
        return ResponseEntity.ok(serviceService.findAllActivePaged(pageable));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<AgencyServiceDTO> getServiceBySlug(@PathVariable String slug) {
        return serviceService.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgencyServiceDTO> createService(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgencyServiceDTO> updateService(@PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serviceService.update(id, request));
    }

    /**
     * PATCH /api/v1/services/{id} — Partial update (ADMIN ONLY).
     * Toggle active, change displayOrder, or update individual fields.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgencyServiceDTO> patchService(@PathVariable Long id,
            @RequestBody ServicePatchRequest request) {
        return ResponseEntity.ok(serviceService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
