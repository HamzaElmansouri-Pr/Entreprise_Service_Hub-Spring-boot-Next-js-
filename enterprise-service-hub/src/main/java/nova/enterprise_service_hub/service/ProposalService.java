package nova.enterprise_service_hub.service;

import jakarta.persistence.EntityNotFoundException;
import nova.enterprise_service_hub.dto.*;
import nova.enterprise_service_hub.model.*;
import nova.enterprise_service_hub.repository.InvoiceRepository;
import nova.enterprise_service_hub.repository.LeadRepository;
import nova.enterprise_service_hub.repository.ProjectRepository;
import nova.enterprise_service_hub.repository.ProposalRepository;
import nova.enterprise_service_hub.security.TenantContext;
import nova.enterprise_service_hub.util.StringSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Proposal Engine — create, send, sign, and auto-convert proposals.
 * <p>
 * The {@link #signProposal} method is the core of the Lead-to-Cash pipeline:
 * it captures the digital signature, creates a Project + Deposit Invoice
 * in a single transaction, and links them back to the proposal — all
 * within the Elite Quality Gate of &lt;100ms.
 */
@Service
@Transactional(readOnly = true)
public class ProposalService {

    private static final Logger log = LoggerFactory.getLogger(ProposalService.class);

    private final ProposalRepository proposalRepository;
    private final LeadRepository leadRepository;
    private final ProjectRepository projectRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;

    public ProposalService(ProposalRepository proposalRepository,
                           LeadRepository leadRepository,
                           ProjectRepository projectRepository,
                           InvoiceRepository invoiceRepository,
                           EmailService emailService) {
        this.proposalRepository = proposalRepository;
        this.leadRepository = leadRepository;
        this.projectRepository = projectRepository;
        this.invoiceRepository = invoiceRepository;
        this.emailService = emailService;
    }

    // ── Queries ──────────────────────────────────────────────────────────

    public List<ProposalDTO> findAll() {
        String tenantId = TenantContext.getTenantId();
        return proposalRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toDTO).toList();
    }

    public PageResponse<ProposalDTO> findAllPaged(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        var page = proposalRepository.findByTenantId(tenantId, pageable).map(this::toDTO);
        return PageResponse.from(page);
    }

    public ProposalDTO findById(Long id) {
        return toDTO(getProposalEntity(id));
    }

    /**
     * Fetch a proposal by its public signing token (no auth required).
     */
    public ProposalDTO findBySigningToken(String token) {
        Proposal p = proposalRepository.findBySigningToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid or expired signing link"));
        return toDTO(p);
    }

    // ── Create Proposal ──────────────────────────────────────────────────

    @Transactional
    public ProposalDTO create(ProposalCreateRequest request) {
        String tenantId = TenantContext.getTenantId();

        Lead lead = leadRepository.findByIdAndTenantId(request.leadId(), tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found: " + request.leadId()));

        Proposal proposal = new Proposal();
        proposal.setTenantId(tenantId);
        proposal.setProposalNumber(generateProposalNumber());
        proposal.setLead(lead);
        proposal.setTitle(StringSanitizer.stripAll(request.title()));
        proposal.setScopeOfWork(StringSanitizer.stripAll(request.scopeOfWork()));
        proposal.setDeliverables(StringSanitizer.stripAll(request.deliverables()));
        proposal.setTermsAndConditions(StringSanitizer.stripAll(request.termsAndConditions()));
        proposal.setDepositPercent(request.depositPercent() > 0 ? request.depositPercent() : 30);
        proposal.setCurrency(request.currency() != null ? request.currency() : "USD");
        proposal.setValidUntil(request.validUntil() != null ? request.validUntil() : LocalDate.now().plusDays(30));
        proposal.setSigningToken(UUID.randomUUID().toString());

        // Line items
        BigDecimal total = BigDecimal.ZERO;
        if (request.lineItems() != null) {
            for (ProposalCreateRequest.LineItemRequest li : request.lineItems()) {
                ProposalLineItem item = new ProposalLineItem();
                item.setProposal(proposal);
                item.setDescription(StringSanitizer.stripAll(li.description()));
                item.setQuantity(li.quantity() > 0 ? li.quantity() : 1);
                item.setUnitPrice(li.unitPrice());
                item.setDisplayOrder(li.displayOrder());
                proposal.getLineItems().add(item);
                total = total.add(item.getLineTotal());
            }
        }
        proposal.setTotalAmount(total);

        // Mark lead as proposal-sent
        lead.setStatus(Lead.LeadStatus.PROPOSAL_SENT);
        leadRepository.save(lead);

        Proposal saved = proposalRepository.save(proposal);
        log.info("Proposal {} created for lead {} (total: {})", saved.getProposalNumber(), lead.getId(), total);
        return toDTO(saved);
    }

    // ── Send Proposal ────────────────────────────────────────────────────

    @Transactional
    public ProposalDTO sendProposal(Long id) {
        Proposal proposal = getProposalEntity(id);
        if (proposal.getStatus() != Proposal.ProposalStatus.DRAFT) {
            throw new IllegalStateException("Can only send DRAFT proposals");
        }
        proposal.setStatus(Proposal.ProposalStatus.SENT);
        proposalRepository.save(proposal);

        // Send signing link email to the lead
        String signingUrl = "https://app.enterprisehub.com/portal/sign/" + proposal.getSigningToken();
        emailService.sendSimpleEmail(
                proposal.getLead().getEmail(),
                "Proposal: " + proposal.getTitle() + " — Ready for Review",
                String.format("""
                        Dear %s,

                        Your proposal "%s" (Ref: %s) is ready for review.

                        Total amount: %s %s
                        Valid until: %s

                        Review and sign here: %s

                        Best regards,
                        Enterprise Service Hub
                        """,
                        proposal.getLead().getFullName(),
                        proposal.getTitle(),
                        proposal.getProposalNumber(),
                        proposal.getCurrency(),
                        proposal.getTotalAmount(),
                        proposal.getValidUntil(),
                        signingUrl));

        log.info("Proposal {} sent to {}", proposal.getProposalNumber(), proposal.getLead().getEmail());
        return toDTO(proposal);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ██ AUTOMATED CONVERSION — The Lead-to-Cash Core                    ██
    // ══════════════════════════════════════════════════════════════════════
    //
    // When a client signs a proposal, this single @Transactional method:
    //   1. Captures the digital signature
    //   2. Creates a new Project workspace
    //   3. Generates a Deposit Invoice
    //   4. Links everything back to the proposal
    //   5. Updates the lead status to WON
    //
    // All within a single DB transaction → sub-100ms with virtual threads.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Sign a proposal and auto-convert to Project + Invoice.
     *
     * @param signingToken the public signing token from the URL
     * @param request      the signature data (name, email, base64 signature PNG)
     * @param signerIp     the IP address of the signer for audit trail
     * @return the updated proposal DTO with conversion links
     */
    @Transactional
    public ProposalDTO signProposal(String signingToken, ProposalSignRequest request, String signerIp) {
        long start = System.nanoTime();

        // ── 1. Validate & capture signature ──────────────────────────────
        Proposal proposal = proposalRepository.findBySigningToken(signingToken)
                .orElseThrow(() -> new EntityNotFoundException("Invalid signing link"));

        if (proposal.getStatus() == Proposal.ProposalStatus.SIGNED
                || proposal.getStatus() == Proposal.ProposalStatus.CONVERTED) {
            throw new IllegalStateException("This proposal has already been signed");
        }

        if (proposal.getValidUntil() != null && proposal.getValidUntil().isBefore(LocalDate.now())) {
            proposal.setStatus(Proposal.ProposalStatus.EXPIRED);
            proposalRepository.save(proposal);
            throw new IllegalStateException("This proposal has expired");
        }

        proposal.setSignerName(request.signerName());
        proposal.setSignerEmail(request.signerEmail());
        proposal.setSignatureData(request.signatureData());
        proposal.setSignedAt(Instant.now());
        proposal.setSignerIp(signerIp);
        proposal.setStatus(Proposal.ProposalStatus.SIGNED);

        // ── 2. Create Project workspace ──────────────────────────────────
        Lead lead = proposal.getLead();
        Project project = new Project();
        project.setTenantId(proposal.getTenantId());
        project.setName(proposal.getTitle());
        project.setClientName(lead.getCompanyName() != null ? lead.getCompanyName() : lead.getFullName());
        project.setCaseStudyChallenge(lead.getProjectDescription());
        project.setDisplayOrder(0);
        project.setArchived(false);
        project.setTechnologies(new ArrayList<>());
        Project savedProject = projectRepository.save(project);

        // ── 3. Generate Deposit Invoice ──────────────────────────────────
        BigDecimal depositAmount = proposal.getTotalAmount()
                .multiply(BigDecimal.valueOf(proposal.getDepositPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Invoice invoice = new Invoice();
        invoice.setTenantId(proposal.getTenantId());
        invoice.setReferenceNumber("DEP-" + proposal.getProposalNumber());
        invoice.setAmount(depositAmount);
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setDueDate(LocalDate.now().plusDays(7));
        invoice.setProject(savedProject);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // ── 4. Link back to proposal ─────────────────────────────────────
        proposal.setConvertedProject(savedProject);
        proposal.setDepositInvoice(savedInvoice);
        proposal.setStatus(Proposal.ProposalStatus.CONVERTED);

        // ── 5. Update lead status ────────────────────────────────────────
        lead.setStatus(Lead.LeadStatus.WON);
        leadRepository.save(lead);

        Proposal saved = proposalRepository.save(proposal);

        long elapsed = (System.nanoTime() - start) / 1_000_000;
        log.info("✅ Proposal {} signed & converted → Project #{}, Invoice {} in {}ms",
                saved.getProposalNumber(), savedProject.getId(), savedInvoice.getReferenceNumber(), elapsed);

        // Async email notification (doesn't block the transaction)
        emailService.sendSimpleEmail(lead.getEmail(),
                "Contract Signed — Project " + savedProject.getName() + " Created!",
                String.format("""
                        Dear %s,

                        Thank you for signing proposal %s!

                        Your project workspace has been created:
                        • Project: %s
                        • Deposit Invoice: %s (%s %s)
                        • Due Date: %s

                        You can track your project and pay invoices at:
                        https://app.enterprisehub.com/portal

                        Welcome aboard!
                        Enterprise Service Hub
                        """,
                        lead.getFullName(),
                        proposal.getProposalNumber(),
                        savedProject.getName(),
                        savedInvoice.getReferenceNumber(),
                        proposal.getCurrency(),
                        depositAmount,
                        savedInvoice.getDueDate()));

        return toDTO(saved);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Proposal getProposalEntity(Long id) {
        String tenantId = TenantContext.getTenantId();
        return proposalRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + id));
    }

    private String generateProposalNumber() {
        return "PRP-" + System.currentTimeMillis() % 1_000_000_000;
    }

    private ProposalDTO toDTO(Proposal p) {
        List<ProposalDTO.LineItemDTO> items = p.getLineItems().stream()
                .map(li -> new ProposalDTO.LineItemDTO(
                        li.getId(), li.getDescription(), li.getQuantity(),
                        li.getUnitPrice(), li.getLineTotal(), li.getDisplayOrder()))
                .toList();

        return new ProposalDTO(
                p.getId(),
                p.getProposalNumber(),
                p.getLead().getId(),
                p.getLead().getFullName(),
                p.getLead().getEmail(),
                p.getTitle(),
                p.getScopeOfWork(),
                p.getDeliverables(),
                p.getTermsAndConditions(),
                items,
                p.getTotalAmount(),
                p.getDepositPercent(),
                p.getCurrency(),
                p.getValidUntil(),
                p.getStatus().name(),
                p.getSignerName(),
                p.getSignerEmail(),
                p.getSignedAt(),
                p.getConvertedProject() != null ? p.getConvertedProject().getId() : null,
                p.getDepositInvoice() != null ? p.getDepositInvoice().getId() : null,
                p.getSigningToken(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
