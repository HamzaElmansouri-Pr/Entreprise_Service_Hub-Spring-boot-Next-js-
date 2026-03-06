package nova.enterprise_service_hub.service;

import jakarta.persistence.EntityNotFoundException;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.dto.ProjectDTO;
import nova.enterprise_service_hub.dto.ProjectPatchRequest;
import nova.enterprise_service_hub.dto.ProjectRequest;
import nova.enterprise_service_hub.mapper.ProjectMapper;
import nova.enterprise_service_hub.model.Project;
import nova.enterprise_service_hub.util.StringSanitizer;
import nova.enterprise_service_hub.event.ProjectCompletedEvent;
import nova.enterprise_service_hub.repository.ProjectRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for {@link Project} operations.
 * Returns DTOs via MapStruct — never leaks entities to the controller.
 * Uses soft deletion (archived) instead of hard delete.
 */
@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository repository;
    private final ProjectMapper mapper;
    private final AssetService assetService;
    private final ApplicationEventPublisher eventPublisher;

    public ProjectService(ProjectRepository repository,
            ProjectMapper mapper,
            AssetService assetService,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.assetService = assetService;
        this.eventPublisher = eventPublisher;
    }

    @Cacheable(value = "projects", key = "'all'")
    public List<ProjectDTO> findAll() {
        return mapper.toDtoList(repository.findAllByArchivedFalseOrderByDisplayOrderAscCreatedAtDesc());
    }

    public PageResponse<ProjectDTO> findAllPaged(Pageable pageable) {
        Page<ProjectDTO> page = repository.findAllByArchivedFalse(pageable).map(mapper::toDto);
        return PageResponse.from(page);
    }

    public ProjectDTO findById(Long id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id)));
    }

    public List<ProjectDTO> findByTechnology(String technology) {
        return mapper.toDtoList(repository.findByTechnology(technology));
    }

    public List<ProjectDTO> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        return mapper.toDtoList(repository.searchByQuery(query.trim()));
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectDTO create(ProjectRequest request) {
        Project project = new Project();
        project.setName(StringSanitizer.stripAll(request.name()));
        project.setClientName(StringSanitizer.stripAll(request.clientName()));
        project.setCaseStudyChallenge(StringSanitizer.stripAll(request.caseStudyChallenge()));
        project.setCaseStudySolution(StringSanitizer.stripAll(request.caseStudySolution()));
        project.setCaseStudyResult(StringSanitizer.stripAll(request.caseStudyResult()));
        project.setImage(assetService.validateAndBuild(request.image()));
        project.setTechnologies(
                request.technologies() != null ? new ArrayList<>(request.technologies()) : new ArrayList<>());
        Project saved = repository.save(project);

        // Publish event for gamification XP award
        if (saved.getTechnologies() != null && !saved.getTechnologies().isEmpty()) {
            eventPublisher.publishEvent(new ProjectCompletedEvent(
                    this, saved.getTenantId(), saved.getId(),
                    saved.getName(), saved.getTechnologies()));
        }

        return mapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectDTO update(Long id, ProjectRequest request) {
        Project project = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
        project.setName(StringSanitizer.stripAll(request.name()));
        project.setClientName(StringSanitizer.stripAll(request.clientName()));
        project.setCaseStudyChallenge(StringSanitizer.stripAll(request.caseStudyChallenge()));
        project.setCaseStudySolution(StringSanitizer.stripAll(request.caseStudySolution()));
        project.setCaseStudyResult(StringSanitizer.stripAll(request.caseStudyResult()));
        project.setImage(assetService.validateAndBuild(request.image()));
        if (request.technologies() != null) {
            project.setTechnologies(new ArrayList<>(request.technologies()));
        }
        return mapper.toDto(repository.save(project));
    }

    /**
     * PATCH — applies only non-null fields from the request.
     * Enables single-field updates like archiving or changing displayOrder.
     */
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectDTO patch(Long id, ProjectPatchRequest request) {
        Project project = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));

        if (request.name() != null) {
            project.setName(StringSanitizer.stripAll(request.name()));
        }
        if (request.clientName() != null) {
            project.setClientName(StringSanitizer.stripAll(request.clientName()));
        }
        if (request.caseStudyChallenge() != null) {
            project.setCaseStudyChallenge(StringSanitizer.stripAll(request.caseStudyChallenge()));
        }
        if (request.caseStudySolution() != null) {
            project.setCaseStudySolution(StringSanitizer.stripAll(request.caseStudySolution()));
        }
        if (request.caseStudyResult() != null) {
            project.setCaseStudyResult(StringSanitizer.stripAll(request.caseStudyResult()));
        }
        if (request.image() != null) {
            project.setImage(assetService.validateAndBuild(request.image()));
        }
        if (request.displayOrder() != null) {
            project.setDisplayOrder(request.displayOrder());
        }
        if (request.archived() != null) {
            project.setArchived(request.archived());
        }
        if (request.technologies() != null) {
            project.setTechnologies(new ArrayList<>(request.technologies()));
        }

        return mapper.toDto(repository.save(project));
    }

    /**
     * Soft-delete: archives the project instead of removing it from the database.
     */
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public void delete(Long id) {
        Project project = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
        project.setArchived(true);
        repository.save(project);
    }
}
