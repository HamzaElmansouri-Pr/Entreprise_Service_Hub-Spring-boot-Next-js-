package nova.enterprise_service_hub.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.dto.ProposalCreateRequest;
import nova.enterprise_service_hub.dto.ProposalDTO;
import nova.enterprise_service_hub.dto.ProposalSignRequest;
import nova.enterprise_service_hub.service.ProposalService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Proposal Controller — create, send, and digitally sign proposals.
 * <p>
 * Admin endpoints: CRUD + send
 * Public endpoints: view by signing token + sign
 */
@RestController
@RequestMapping("/v1/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    // ── Admin Endpoints ──────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProposalDTO> getAllProposals() {
        return proposalService.findAll();
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ProposalDTO> getProposalsPaged(@PageableDefault(size = 20) Pageable pageable) {
        return proposalService.findAllPaged(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProposalDTO getProposal(@PathVariable Long id) {
        return proposalService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProposalDTO> createProposal(@Valid @RequestBody ProposalCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proposalService.create(request));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ProposalDTO sendProposal(@PathVariable Long id) {
        return proposalService.sendProposal(id);
    }

    // ── Public Endpoints (signing flow — no auth required) ───────────────

    @GetMapping("/sign/{token}")
    public ProposalDTO getProposalByToken(@PathVariable String token) {
        return proposalService.findBySigningToken(token);
    }

    /**
     * Digital signature + automated conversion to Project + Invoice.
     * This is the Lead-to-Cash conversion core.
     */
    @PostMapping("/sign/{token}")
    public ProposalDTO signProposal(
            @PathVariable String token,
            @Valid @RequestBody ProposalSignRequest request,
            HttpServletRequest httpRequest) {
        String signerIp = httpRequest.getHeader("X-Forwarded-For");
        if (signerIp == null) signerIp = httpRequest.getRemoteAddr();
        return proposalService.signProposal(token, request, signerIp);
    }
}
