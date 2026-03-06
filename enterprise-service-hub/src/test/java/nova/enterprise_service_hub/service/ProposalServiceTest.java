package nova.enterprise_service_hub.service;

import jakarta.persistence.EntityNotFoundException;
import nova.enterprise_service_hub.dto.*;
import nova.enterprise_service_hub.model.*;
import nova.enterprise_service_hub.repository.InvoiceRepository;
import nova.enterprise_service_hub.repository.LeadRepository;
import nova.enterprise_service_hub.repository.ProjectRepository;
import nova.enterprise_service_hub.repository.ProposalRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProposalService — the Lead-to-Cash conversion engine.
 */
@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock private ProposalRepository proposalRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private ProposalService proposalService;

    private static final String TENANT_ID = "test-tenant";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Lead createLead() {
        Lead lead = new Lead();
        lead.setId(1L);
        lead.setTenantId(TENANT_ID);
        lead.setFullName("Alice Johnson");
        lead.setEmail("alice@example.com");
        lead.setCompanyName("Acme Corp");
        lead.setProjectDescription("Build an enterprise dashboard");
        lead.setStatus(Lead.LeadStatus.QUALIFIED);
        return lead;
    }

    private Proposal createProposal(Lead lead) {
        Proposal p = new Proposal();
        p.setId(1L);
        p.setTenantId(TENANT_ID);
        p.setProposalNumber("PRP-123456");
        p.setLead(lead);
        p.setTitle("Enterprise Dashboard Proposal");
        p.setScopeOfWork("Full stack dashboard development");
        p.setStatus(Proposal.ProposalStatus.DRAFT);
        p.setTotalAmount(BigDecimal.valueOf(50_000));
        p.setDepositPercent(30);
        p.setCurrency("USD");
        p.setValidUntil(LocalDate.now().plusDays(30));
        p.setSigningToken("test-token-123");
        p.setLineItems(new ArrayList<>());
        return p;
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return proposals for current tenant")
        void returnsAll() {
            Lead lead = createLead();
            Proposal p = createProposal(lead);
            when(proposalRepository.findByTenantIdOrderByCreatedAtDesc(TENANT_ID)).thenReturn(List.of(p));

            List<ProposalDTO> result = proposalService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Enterprise Dashboard Proposal");
        }
    }

    @Nested
    @DisplayName("findBySigningToken")
    class FindBySigningToken {

        @Test
        @DisplayName("should return proposal for valid token")
        void validToken() {
            Lead lead = createLead();
            Proposal p = createProposal(lead);
            when(proposalRepository.findBySigningToken("test-token-123")).thenReturn(Optional.of(p));

            ProposalDTO result = proposalService.findBySigningToken("test-token-123");

            assertThat(result.proposalNumber()).isEqualTo("PRP-123456");
        }

        @Test
        @DisplayName("should throw for invalid token")
        void invalidToken() {
            when(proposalRepository.findBySigningToken("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> proposalService.findBySigningToken("invalid"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create proposal with line items from lead")
        void createsProposal() {
            Lead lead = createLead();
            when(leadRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(lead));
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> {
                Proposal saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            var lineItem = new ProposalCreateRequest.LineItemRequest(
                    "Dashboard development", 1, BigDecimal.valueOf(50_000), 0);
            ProposalCreateRequest request = new ProposalCreateRequest(
                    1L, "Dashboard Proposal", "Build full-stack dashboard",
                    "MVP + production deploy", "Standard terms",
                    List.of(lineItem), 30, "USD", LocalDate.now().plusDays(14));

            ProposalDTO result = proposalService.create(request);

            assertThat(result.title()).isEqualTo("Dashboard Proposal");
            assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
            assertThat(result.lineItems()).hasSize(1);
            verify(leadRepository).save(argThat(l -> l.getStatus() == Lead.LeadStatus.PROPOSAL_SENT));
        }

        @Test
        @DisplayName("should throw when lead not found")
        void throwsWhenLeadMissing() {
            when(leadRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

            ProposalCreateRequest request = new ProposalCreateRequest(
                    99L, "Title", null, null, null, null, 30, "USD", null);

            assertThatThrownBy(() -> proposalService.create(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("sendProposal")
    class SendProposal {

        @Test
        @DisplayName("should send draft proposal and update status to SENT")
        void sendsDraft() {
            Lead lead = createLead();
            Proposal p = createProposal(lead);
            when(proposalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(p));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

            ProposalDTO result = proposalService.sendProposal(1L);

            assertThat(result.status()).isEqualTo("SENT");
            verify(emailService).sendSimpleEmail(eq("alice@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw when proposal is not DRAFT")
        void throwsWhenNotDraft() {
            Lead lead = createLead();
            Proposal p = createProposal(lead);
            p.setStatus(Proposal.ProposalStatus.SENT);
            when(proposalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> proposalService.sendProposal(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT");
        }
    }

    @Nested
    @DisplayName("signProposal — Lead-to-Cash Conversion")
    class SignProposal {

        @Test
        @DisplayName("should convert proposal to Project + Invoice in single transaction")
        void signAndConvert() {
            Lead lead = createLead();
            Proposal p = createProposal(lead);
            p.setStatus(Proposal.ProposalStatus.SENT);

            when(proposalRepository.findBySigningToken("test-token-123")).thenReturn(Optional.of(p));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
                Project proj = inv.getArgument(0);
                proj.setId(10L);
                return proj;
            });
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
                Invoice invoice = inv.getArgument(0);
                invoice.setId(20L);
                return invoice;
            });
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

            ProposalSignRequest signRequest = new ProposalSignRequest(
                    "Alice Johnson", "alice@example.com", "data:image/png;base64,iVBOR...");

            ProposalDTO result = proposalService.signProposal("test-token-123", signRequest, "192.168.1.1");

            // Verify conversion
            assertThat(result.status()).isEqualTo("CONVERTED");
            assertThat(result.convertedProjectId()).isEqualTo(10L);
            assertThat(result.depositInvoiceId()).isEqualTo(20L);
            assertThat(result.signerName()).isEqualTo("Alice Johnson");

            // Verify Project created
            verify(projectRepository).save(argThat(proj ->
                    proj.getName().equals("Enterprise Dashboard Proposal") &&
                    proj.getClientName().equals("Acme Corp") &&
                    proj.getTenantId().equals(TENANT_ID)));

            // Verify Invoice created with correct deposit amount
            verify(invoiceRepository).save(argThat(inv ->
                    inv.getAmount().compareTo(BigDecimal.valueOf(15_000.00)) == 0 &&
                    inv.getReferenceNumber().startsWith("DEP-") &&
                    inv.getStatus() == Invoice.InvoiceStatus.PENDING));

            // Verify Lead marked as WON
            verify(leadRepository).save(argThat(l -> l.getStatus() == Lead.LeadStatus.WON));

            // Verify email sent
            verify(emailService).sendSimpleEmail(eq("alice@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("should reject already signed proposals")
        void rejectsAlreadySigned() {
            Lead lead = createLead();
            Proposal p = createProposal(lead);
            p.setStatus(Proposal.ProposalStatus.SIGNED);
            when(proposalRepository.findBySigningToken("test-token-123")).thenReturn(Optional.of(p));

            ProposalSignRequest req = new ProposalSignRequest("Alice", "alice@test.com", "sig");

            assertThatThrownBy(() -> proposalService.signProposal("test-token-123", req, "1.2.3.4"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already been signed");
        }

        @Test
        @DisplayName("should reject expired proposals and update status")
        void rejectsExpired() {
            Lead lead = createLead();
            Proposal p = createProposal(lead);
            p.setStatus(Proposal.ProposalStatus.SENT);
            p.setValidUntil(LocalDate.now().minusDays(1)); // expired
            when(proposalRepository.findBySigningToken("test-token-123")).thenReturn(Optional.of(p));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

            ProposalSignRequest req = new ProposalSignRequest("Alice", "alice@test.com", "sig");

            assertThatThrownBy(() -> proposalService.signProposal("test-token-123", req, "1.2.3.4"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("expired");

            verify(proposalRepository).save(argThat(prop ->
                    prop.getStatus() == Proposal.ProposalStatus.EXPIRED));
        }

        @Test
        @DisplayName("should throw for invalid signing token")
        void invalidToken() {
            when(proposalRepository.findBySigningToken("bad-token")).thenReturn(Optional.empty());

            ProposalSignRequest req = new ProposalSignRequest("Alice", "alice@test.com", "sig");

            assertThatThrownBy(() -> proposalService.signProposal("bad-token", req, "1.2.3.4"))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("deposit should be 30% of total by default")
        void depositCalculation() {
            Lead lead = createLead();
            Proposal p = createProposal(lead);
            p.setStatus(Proposal.ProposalStatus.SENT);
            p.setTotalAmount(BigDecimal.valueOf(100_000));
            p.setDepositPercent(30);

            when(proposalRepository.findBySigningToken("test-token-123")).thenReturn(Optional.of(p));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
                Project proj = inv.getArgument(0);
                proj.setId(1L);
                return proj;
            });
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
                Invoice invoice = inv.getArgument(0);
                invoice.setId(1L);
                return invoice;
            });
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

            ProposalSignRequest req = new ProposalSignRequest("Alice", "alice@test.com", "sig");
            proposalService.signProposal("test-token-123", req, "1.2.3.4");

            verify(invoiceRepository).save(argThat(inv ->
                    inv.getAmount().compareTo(BigDecimal.valueOf(30_000.00)) == 0));
        }

        @Test
        @DisplayName("uses company name as project client, falls back to full name")
        void fallbackClientName() {
            Lead lead = createLead();
            lead.setCompanyName(null); // no company
            Proposal p = createProposal(lead);
            p.setStatus(Proposal.ProposalStatus.SENT);

            when(proposalRepository.findBySigningToken("test-token-123")).thenReturn(Optional.of(p));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
                Project proj = inv.getArgument(0);
                proj.setId(1L);
                return proj;
            });
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
                Invoice invoice = inv.getArgument(0);
                invoice.setId(1L);
                return invoice;
            });
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

            ProposalSignRequest req = new ProposalSignRequest("Alice", "alice@test.com", "sig");
            proposalService.signProposal("test-token-123", req, "1.2.3.4");

            verify(projectRepository).save(argThat(proj ->
                    proj.getClientName().equals("Alice Johnson")));
        }
    }
}
