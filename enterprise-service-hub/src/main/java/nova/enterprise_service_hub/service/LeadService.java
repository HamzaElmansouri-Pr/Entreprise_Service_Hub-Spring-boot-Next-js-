package nova.enterprise_service_hub.service;

import jakarta.persistence.EntityNotFoundException;
import nova.enterprise_service_hub.dto.LeadCreateRequest;
import nova.enterprise_service_hub.dto.LeadDTO;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.model.Lead;
import nova.enterprise_service_hub.repository.LeadRepository;
import nova.enterprise_service_hub.security.TenantContext;
import nova.enterprise_service_hub.util.StringSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lead Management Service — CRUD + AI scoring for inbound leads.
 */
@Service
@Transactional(readOnly = true)
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final LeadScoringService scoringService;

    public LeadService(LeadRepository leadRepository, LeadScoringService scoringService) {
        this.leadRepository = leadRepository;
        this.scoringService = scoringService;
    }

    public List<LeadDTO> findAll() {
        String tenantId = TenantContext.getTenantId();
        return leadRepository.findByTenantIdOrderByScoreDesc(tenantId)
                .stream().map(this::toDTO).toList();
    }

    public PageResponse<LeadDTO> findAllPaged(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        var page = leadRepository.findByTenantId(tenantId, pageable).map(this::toDTO);
        return PageResponse.from(page);
    }

    public LeadDTO findById(Long id) {
        return toDTO(getLeadEntity(id));
    }

    public List<LeadDTO> findHotLeads(int minScore) {
        String tenantId = TenantContext.getTenantId();
        return leadRepository.findHotLeads(tenantId, minScore)
                .stream().map(this::toDTO).toList();
    }

    /**
     * Create a new lead from the contact form and compute AI score.
     */
    @Transactional
    public LeadDTO create(LeadCreateRequest request) {
        Lead lead = new Lead();
        lead.setFullName(StringSanitizer.stripAll(request.fullName()));
        lead.setEmail(request.email().trim().toLowerCase());
        lead.setPhone(request.phone());
        lead.setCompanyName(StringSanitizer.stripAll(request.companyName()));
        lead.setProjectTitle(StringSanitizer.stripAll(request.projectTitle()));
        lead.setProjectDescription(StringSanitizer.stripAll(request.projectDescription()));
        lead.setBudget(request.budget());
        lead.setTimeline(request.timeline());

        // AI Lead Scoring
        LeadScoringService.ScoringResult result = scoringService.score(lead);
        lead.setScore(result.score());
        lead.setScoreBreakdown(result.breakdown());

        Lead saved = leadRepository.save(lead);
        log.info("Lead created: {} (score: {})", saved.getId(), saved.getScore());
        return toDTO(saved);
    }

    @Transactional
    public LeadDTO updateStatus(Long id, Lead.LeadStatus status) {
        Lead lead = getLeadEntity(id);
        lead.setStatus(status);
        return toDTO(leadRepository.save(lead));
    }

    @Transactional
    public LeadDTO addNote(Long id, String note) {
        Lead lead = getLeadEntity(id);
        String existing = lead.getNotes() != null ? lead.getNotes() + "\n---\n" : "";
        lead.setNotes(existing + StringSanitizer.stripAll(note));
        return toDTO(leadRepository.save(lead));
    }

    @Transactional
    public LeadDTO assignTo(Long id, String assignee) {
        Lead lead = getLeadEntity(id);
        lead.setAssignedTo(assignee);
        return toDTO(leadRepository.save(lead));
    }

    @Transactional
    public LeadDTO rescore(Long id) {
        Lead lead = getLeadEntity(id);
        LeadScoringService.ScoringResult result = scoringService.score(lead);
        lead.setScore(result.score());
        lead.setScoreBreakdown(result.breakdown());
        return toDTO(leadRepository.save(lead));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Lead getLeadEntity(Long id) {
        String tenantId = TenantContext.getTenantId();
        return leadRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found: " + id));
    }

    private LeadDTO toDTO(Lead l) {
        return new LeadDTO(
                l.getId(), l.getFullName(), l.getEmail(), l.getPhone(),
                l.getCompanyName(), l.getProjectTitle(), l.getProjectDescription(),
                l.getBudget(), l.getTimeline(), l.getScore(), l.getScoreBreakdown(),
                l.getStatus().name(), l.getNotes(), l.getAssignedTo(),
                l.getCreatedAt(), l.getUpdatedAt());
    }
}
