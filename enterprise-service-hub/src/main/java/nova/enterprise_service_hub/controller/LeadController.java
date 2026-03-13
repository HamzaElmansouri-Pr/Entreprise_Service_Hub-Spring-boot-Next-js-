package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.LeadCreateRequest;
import nova.enterprise_service_hub.dto.LeadDTO;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.model.Lead;
import nova.enterprise_service_hub.service.LeadService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Lead Management Controller — ingest and manage sales leads.
 * <p>
 * POST /v1/leads (public — contact form)
 * All other endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/v1/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    /**
     * Public endpoint — contact form submission creates a scored lead.
     */
    @PostMapping
    public ResponseEntity<LeadDTO> createLead(@Valid @RequestBody LeadCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<LeadDTO> getAllLeads() {
        return leadService.findAll();
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<LeadDTO> getLeadsPaged(@PageableDefault(size = 20) Pageable pageable) {
        return leadService.findAllPaged(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public LeadDTO getLead(@PathVariable Long id) {
        return leadService.findById(id);
    }

    @GetMapping("/hot")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LeadDTO> getHotLeads(@RequestParam(defaultValue = "70") int minScore) {
        return leadService.findHotLeads(minScore);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LeadDTO updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Lead.LeadStatus status = Lead.LeadStatus.valueOf(body.get("status"));
        return leadService.updateStatus(id, status);
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LeadDTO addNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return leadService.addNote(id, body.get("note"));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public LeadDTO assignLead(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return leadService.assignTo(id, body.get("assignee"));
    }

    @PostMapping("/{id}/rescore")
    @PreAuthorize("hasRole('ADMIN')")
    public LeadDTO rescoreLead(@PathVariable Long id) {
        return leadService.rescore(id);
    }
}
