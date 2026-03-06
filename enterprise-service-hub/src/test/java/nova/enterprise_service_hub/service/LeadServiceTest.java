package nova.enterprise_service_hub.service;

import jakarta.persistence.EntityNotFoundException;
import nova.enterprise_service_hub.dto.LeadCreateRequest;
import nova.enterprise_service_hub.dto.LeadDTO;
import nova.enterprise_service_hub.model.Lead;
import nova.enterprise_service_hub.repository.LeadRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LeadService — CRUD + AI scoring integration.
 */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private LeadScoringService scoringService;

    @InjectMocks
    private LeadService leadService;

    private static final String TENANT_ID = "test-tenant";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Lead createLead(Long id, String name, int score) {
        Lead lead = new Lead();
        lead.setId(id);
        lead.setTenantId(TENANT_ID);
        lead.setFullName(name);
        lead.setEmail(name.toLowerCase().replace(" ", "") + "@test.com");
        lead.setCompanyName("Test Corp");
        lead.setScore(score);
        lead.setStatus(Lead.LeadStatus.NEW);
        lead.setScoreBreakdown("Budget: 30/35 | Description: 15/30 | Company: 15/15 | Contact: 10/10 | Timeline: 0/10");
        return lead;
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return leads sorted by score desc")
        void returnsLeadsSorted() {
            Lead l1 = createLead(1L, "Alice Smith", 85);
            Lead l2 = createLead(2L, "Bob Jones", 42);
            when(leadRepository.findByTenantIdOrderByScoreDesc(TENANT_ID)).thenReturn(List.of(l1, l2));

            List<LeadDTO> result = leadService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).fullName()).isEqualTo("Alice Smith");
            assertThat(result.get(0).score()).isEqualTo(85);
        }

        @Test
        @DisplayName("should return empty list when no leads")
        void emptyList() {
            when(leadRepository.findByTenantIdOrderByScoreDesc(TENANT_ID)).thenReturn(List.of());

            List<LeadDTO> result = leadService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return lead when found")
        void returnsLead() {
            Lead lead = createLead(1L, "Alice Smith", 85);
            when(leadRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(lead));

            LeadDTO result = leadService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.fullName()).isEqualTo("Alice Smith");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when not found")
        void throwsWhenNotFound() {
            when(leadRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> leadService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create lead and compute AI score")
        void createsWithScore() {
            LeadCreateRequest request = new LeadCreateRequest(
                    "Jane Doe", "jane@company.com", "+1-555-0100",
                    "Acme Corp", "Enterprise Dashboard", "Build a cloud analytics dashboard",
                    BigDecimal.valueOf(75_000), "3 months"
            );

            var scoringResult = new LeadScoringService.ScoringResult(72, "Budget: 30/35 | Description: 12/30 | Company: 15/15 | Contact: 10/10 | Timeline: 5/10");
            when(scoringService.score(any(Lead.class))).thenReturn(scoringResult);
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
                Lead saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            LeadDTO result = leadService.create(request);

            assertThat(result.score()).isEqualTo(72);
            assertThat(result.email()).isEqualTo("jane@company.com");
            verify(scoringService).score(any(Lead.class));
            verify(leadRepository).save(any(Lead.class));
        }

        @Test
        @DisplayName("should sanitize input fields")
        void sanitizesInput() {
            LeadCreateRequest request = new LeadCreateRequest(
                    "<script>alert('xss')</script>John", "john@test.com", null,
                    null, "<b>Bold Title</b>", "<script>hack()</script>",
                    null, null
            );

            when(scoringService.score(any(Lead.class)))
                    .thenReturn(new LeadScoringService.ScoringResult(5, "low"));
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
                Lead saved = inv.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            LeadDTO result = leadService.create(request);

            // Verify sanitizer was applied (the exact sanitization depends on StringSanitizer impl)
            verify(leadRepository).save(argThat(lead ->
                    !lead.getFullName().contains("<script>")));
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should update lead status")
        void updatesStatus() {
            Lead lead = createLead(1L, "Alice Smith", 85);
            when(leadRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(lead));
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

            LeadDTO result = leadService.updateStatus(1L, Lead.LeadStatus.QUALIFIED);

            assertThat(result.status()).isEqualTo("QUALIFIED");
            verify(leadRepository).save(argThat(l -> l.getStatus() == Lead.LeadStatus.QUALIFIED));
        }
    }

    @Nested
    @DisplayName("addNote")
    class AddNote {

        @Test
        @DisplayName("should append note to existing notes")
        void appendsNote() {
            Lead lead = createLead(1L, "Alice Smith", 85);
            lead.setNotes("First note");
            when(leadRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(lead));
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

            leadService.addNote(1L, "Second note");

            verify(leadRepository).save(argThat(l ->
                    l.getNotes().contains("First note") && l.getNotes().contains("Second note")));
        }

        @Test
        @DisplayName("should set note when no existing notes")
        void setsFirstNote() {
            Lead lead = createLead(1L, "Alice Smith", 85);
            lead.setNotes(null);
            when(leadRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(lead));
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

            leadService.addNote(1L, "First note");

            verify(leadRepository).save(argThat(l -> l.getNotes() != null && !l.getNotes().isEmpty()));
        }
    }

    @Nested
    @DisplayName("rescore")
    class Rescore {

        @Test
        @DisplayName("should recalculate and update score")
        void rescoresLead() {
            Lead lead = createLead(1L, "Alice Smith", 50);
            when(leadRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(lead));
            when(scoringService.score(lead))
                    .thenReturn(new LeadScoringService.ScoringResult(92, "improved"));
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

            LeadDTO result = leadService.rescore(1L);

            assertThat(result.score()).isEqualTo(92);
            verify(scoringService).score(lead);
        }
    }

    @Nested
    @DisplayName("findHotLeads")
    class FindHotLeads {

        @Test
        @DisplayName("should return leads with score >= minScore")
        void returnsHotLeads() {
            Lead hot = createLead(1L, "Hot Lead", 90);
            when(leadRepository.findHotLeads(TENANT_ID, 80)).thenReturn(List.of(hot));

            List<LeadDTO> result = leadService.findHotLeads(80);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).score()).isGreaterThanOrEqualTo(80);
        }
    }
}
