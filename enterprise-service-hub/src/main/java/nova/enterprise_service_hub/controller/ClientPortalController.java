package nova.enterprise_service_hub.controller;

import nova.enterprise_service_hub.dto.ClientPortalDTO;
import nova.enterprise_service_hub.dto.ClientProfileUpdateDTO;
import nova.enterprise_service_hub.dto.InvoiceDTO;
import nova.enterprise_service_hub.dto.PortalProjectDetailDTO;
import nova.enterprise_service_hub.model.ClientUser;
import nova.enterprise_service_hub.service.ClientPortalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Client Portal Controller — secure endpoints for external clients.
 * <p>
 * Clients authenticate via email+password (separate from admin JWT),
 * and receive a portal-scoped view of their projects and invoices.
 */
@RestController
@RequestMapping("/v1/portal")
public class ClientPortalController {

    private final ClientPortalService portalService;

    public ClientPortalController(ClientPortalService portalService) {
        this.portalService = portalService;
    }

    /**
     * Client login — returns dashboard data directly.
     */
    @PostMapping("/login")
    public ClientPortalDTO clientLogin(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        ClientUser client = portalService.authenticateClient(email, password);
        return portalService.getDashboard(client.getId());
    }

    /**
     * Get portal dashboard for an authenticated client.
     */
    @GetMapping("/dashboard/{clientId}")
    public ClientPortalDTO getDashboard(@PathVariable Long clientId) {
        return portalService.getDashboard(clientId);
    }

    /**
     * Get invoices for a client.
     */
    @GetMapping("/{clientId}/invoices")
    public List<InvoiceDTO> getClientInvoices(@PathVariable Long clientId) {
        return portalService.getClientInvoices(clientId);
    }

    /**
     * Create a Stripe checkout session for an invoice payment.
     */
    @PostMapping("/{clientId}/invoices/{invoiceId}/pay")
    public ResponseEntity<Map<String, String>> payInvoice(
            @PathVariable Long clientId,
            @PathVariable Long invoiceId) {
        String checkoutUrl = portalService.createInvoiceCheckout(clientId, invoiceId);
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }

    /**
     * Get details of a specific project.
     */
    @GetMapping("/{clientId}/projects/{projectId}")
    public PortalProjectDetailDTO getProjectDetails(
            @PathVariable Long clientId,
            @PathVariable Long projectId) {
        return portalService.getProjectDetails(clientId, projectId);
    }

    /**
     * Update client profile.
     */
    @PutMapping("/{clientId}/profile")
    public ResponseEntity<Void> updateProfile(
            @PathVariable Long clientId,
            @RequestBody ClientProfileUpdateDTO dto) {
        portalService.updateClientProfile(clientId, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Accept an invite and set a password.
     */
    @PostMapping("/accept-invite")
    public ResponseEntity<Map<String, String>> acceptInvite(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String password = body.get("password");
        portalService.acceptInvite(token, password);
        return ResponseEntity.ok(Map.of("message", "Account activated successfully"));
    }
}
