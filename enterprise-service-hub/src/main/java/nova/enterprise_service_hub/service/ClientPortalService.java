package nova.enterprise_service_hub.service;

import jakarta.persistence.EntityNotFoundException;
import nova.enterprise_service_hub.dto.ClientPortalDTO;
import nova.enterprise_service_hub.dto.InvoiceDTO;
import nova.enterprise_service_hub.model.ClientUser;
import nova.enterprise_service_hub.model.Invoice;
import nova.enterprise_service_hub.model.Project;
import nova.enterprise_service_hub.repository.ClientUserRepository;
import nova.enterprise_service_hub.repository.InvoiceRepository;
import nova.enterprise_service_hub.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import nova.enterprise_service_hub.dto.ClientProfileUpdateDTO;
import nova.enterprise_service_hub.dto.PortalProjectDetailDTO;
import nova.enterprise_service_hub.model.ProjectUpdate;
import nova.enterprise_service_hub.model.ProjectFile;

/**
 * Client Portal Service — secure access for external clients
 * to view their project progress and pay invoices.
 */
@Service
@Transactional(readOnly = true)
public class ClientPortalService {

    private static final Logger log = LoggerFactory.getLogger(ClientPortalService.class);

    private final ClientUserRepository clientUserRepository;
    private final ProjectRepository projectRepository;
    private final InvoiceRepository invoiceRepository;
    private final StripeService stripeService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ClientPortalService(ClientUserRepository clientUserRepository,
                               ProjectRepository projectRepository,
                               InvoiceRepository invoiceRepository,
                               StripeService stripeService,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService) {
        this.clientUserRepository = clientUserRepository;
        this.projectRepository = projectRepository;
        this.invoiceRepository = invoiceRepository;
        this.stripeService = stripeService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Authenticate a client user and return their portal dashboard.
     */
    public ClientUser authenticateClient(String email, String password) {
        ClientUser client = clientUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        if (!client.isActive()) {
            throw new IllegalStateException("Account is deactivated");
        }

        if (client.getPasswordHash() == null || !passwordEncoder.matches(password, client.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return client;
    }

    /**
     * Get the full portal dashboard for a client user.
     */
    public ClientPortalDTO getDashboard(Long clientUserId) {
        ClientUser client = clientUserRepository.findById(clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        String tenantId = client.getTenantId();

        // Get projects for this client's company
        List<Project> projects = projectRepository.findByClientNameIgnoreCase(
                client.getCompanyName() != null ? client.getCompanyName() : client.getFullName());

        // Get invoices for the tenant
        List<Invoice> allInvoices = invoiceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        // Filter to invoices linked to client's projects
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        List<Invoice> clientInvoices = allInvoices.stream()
                .filter(inv -> inv.getProject() != null && projectIds.contains(inv.getProject().getId()))
                .toList();

        BigDecimal totalPaid = clientInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PAID)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = clientInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PENDING
                        || inv.getStatus() == Invoice.InvoiceStatus.OVERDUE)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ClientPortalDTO.PortalProject> portalProjects = projects.stream()
                .map(p -> new ClientPortalDTO.PortalProject(
                        p.getId(),
                        p.getName(),
                        p.isArchived() ? "Completed" : "In Progress",
                        p.isArchived() ? 100 : estimateProgress(p),
                        null))
                .toList();

        List<ClientPortalDTO.PortalInvoice> portalInvoices = clientInvoices.stream()
                .map(inv -> {
                    String checkoutUrl = null;
                    if (inv.getStatus() == Invoice.InvoiceStatus.PENDING
                            || inv.getStatus() == Invoice.InvoiceStatus.OVERDUE) {
                        try {
                            checkoutUrl = stripeService.createCheckoutSession(
                                    inv.getReferenceNumber(), inv.getAmount(), "usd", tenantId);
                        } catch (Exception e) {
                            log.warn("Could not create Stripe checkout for {}: {}", inv.getReferenceNumber(), e.getMessage());
                        }
                    }
                    return new ClientPortalDTO.PortalInvoice(
                            inv.getId(),
                            inv.getReferenceNumber(),
                            inv.getAmount(),
                            inv.getStatus().name(),
                            inv.getDueDate().toString(),
                            checkoutUrl);
                })
                .toList();

        return new ClientPortalDTO(
                client.getId(),
                client.getFullName(),
                client.getCompanyName(),
                portalProjects,
                portalInvoices,
                totalPaid,
                totalOutstanding);
    }

    /**
     * Get all invoices as DTOs for a client user.
     */
    public List<InvoiceDTO> getClientInvoices(Long clientUserId) {
        ClientUser client = clientUserRepository.findById(clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        List<Project> projects = projectRepository.findByClientNameIgnoreCase(
                client.getCompanyName() != null ? client.getCompanyName() : client.getFullName());

        List<Long> projectIds = projects.stream().map(Project::getId).toList();

        return invoiceRepository.findByTenantIdOrderByCreatedAtDesc(client.getTenantId()).stream()
                .filter(inv -> inv.getProject() != null && projectIds.contains(inv.getProject().getId()))
                .map(this::toInvoiceDTO)
                .toList();
    }

    /**
     * Create a Stripe checkout session for a specific invoice.
     */
    public String createInvoiceCheckout(Long clientUserId, Long invoiceId) {
        ClientUser client = clientUserRepository.findById(clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, client.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already paid");
        }

        try {
            return stripeService.createCheckoutSession(
                    invoice.getReferenceNumber(), invoice.getAmount(), "usd", client.getTenantId());
        } catch (Exception e) {
            throw new RuntimeException("Payment session creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Register a new client from a proposal acceptance (invite-based).
     */
    @Transactional
    public ClientUser createClientFromLead(String tenantId, String fullName, String email, String companyName) {
        // Check if client already exists
        var existing = clientUserRepository.findByEmail(email.trim().toLowerCase());
        if (existing.isPresent()) {
            return existing.get();
        }

        ClientUser client = new ClientUser();
        client.setTenantId(tenantId);
        client.setFullName(fullName);
        client.setEmail(email.trim().toLowerCase());
        client.setCompanyName(companyName);
        client.setInviteToken(UUID.randomUUID().toString());
        client.setInviteAccepted(false);
        client.setActive(true);

        ClientUser saved = clientUserRepository.save(client);

        emailService.sendClientInvitation(
                saved.getEmail(),
                saved.getFullName(),
                "https://app.enterprisehub.com/portal/accept-invite?token=" + saved.getInviteToken());

        log.info("Client user created for {}: {}", companyName, email);
        return saved;
    }

    /**
     * Accept invite and set password.
     */
    @Transactional
    public ClientUser acceptInvite(String inviteToken, String password) {
        ClientUser client = clientUserRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invite link"));

        if (client.isInviteAccepted()) {
            throw new IllegalStateException("Invite already accepted");
        }

        client.setPasswordHash(passwordEncoder.encode(password));
        client.setInviteAccepted(true);
        client.setInviteToken(null); // invalidate token
        return clientUserRepository.save(client);
    }

    /**
     * Update client profile (name, phone, password).
     */
    @Transactional
    public void updateClientProfile(Long clientUserId, ClientProfileUpdateDTO dto) {
        ClientUser client = clientUserRepository.findById(clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        if (dto.fullName() != null && !dto.fullName().isBlank()) {
            client.setFullName(dto.fullName());
        }
        if (dto.phoneNumber() != null) {
            client.setPhoneNumber(dto.phoneNumber());
        }
        if (dto.password() != null && !dto.password().isBlank()) {
            client.setPasswordHash(passwordEncoder.encode(dto.password()));
        }
        clientUserRepository.save(client);
    }

    /**
     * Get detailed project view for client (including updates and files).
     */
    public PortalProjectDetailDTO getProjectDetails(Long clientUserId, Long projectId) {
        ClientUser client = clientUserRepository.findById(clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        // Security check: ensure project belongs to client's tenant and company
        if (!project.getTenantId().equals(client.getTenantId())) {
            throw new SecurityException("Access denied");
        }
        
        String clientName = client.getCompanyName() != null ? client.getCompanyName() : client.getFullName();
        if (!project.getClientName().equalsIgnoreCase(clientName)) {
            throw new SecurityException("Access denied to this project");
        }

        List<PortalProjectDetailDTO.ProjectUpdateDTO> updates = project.getUpdates().stream()
                .map(u -> new PortalProjectDetailDTO.ProjectUpdateDTO(u.getId(), u.getTitle(), u.getDetail(), u.getTimestamp()))
                .toList();

        List<PortalProjectDetailDTO.ProjectFileDTO> files = project.getFiles().stream()
                .map(f -> new PortalProjectDetailDTO.ProjectFileDTO(
                        f.getId(), f.getFileName(), f.getFileUrl(), f.getFileType(),
                        f.getFileSize() != null ? f.getFileSize() : 0, f.getUploadedAt()))
                .toList();

        String status = project.isArchived() ? "Completed" : "In Progress";
        int progress = project.isArchived() ? 100 : estimateProgress(project);

        String description = project.getCaseStudyChallenge(); // fallback description

        return new PortalProjectDetailDTO(
                project.getId(),
                project.getName(),
                status,
                progress,
                description,
                updates,
                files
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private int estimateProgress(Project project) {
        // Simple heuristic based on case study completeness
        int progress = 10; // base (just created)
        if (project.getCaseStudyChallenge() != null && !project.getCaseStudyChallenge().isBlank()) progress += 20;
        if (project.getCaseStudySolution() != null && !project.getCaseStudySolution().isBlank()) progress += 30;
        if (project.getCaseStudyResult() != null && !project.getCaseStudyResult().isBlank()) progress += 30;
        if (project.getTechnologies() != null && !project.getTechnologies().isEmpty()) progress += 10;
        return Math.min(progress, 95);
    }

    private InvoiceDTO toInvoiceDTO(Invoice inv) {
        return new InvoiceDTO(
                inv.getId(),
                inv.getReferenceNumber(),
                inv.getAmount(),
                inv.getStatus().name(),
                inv.getDueDate(),
                inv.getProject() != null ? inv.getProject().getId() : null,
                inv.getProject() != null ? inv.getProject().getName() : null,
                inv.getCreatedAt(),
                inv.getUpdatedAt());
    }
}
