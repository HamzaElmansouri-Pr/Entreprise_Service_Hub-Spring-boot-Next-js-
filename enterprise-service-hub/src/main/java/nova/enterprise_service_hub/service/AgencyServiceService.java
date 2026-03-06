package nova.enterprise_service_hub.service;

import jakarta.persistence.EntityNotFoundException;
import nova.enterprise_service_hub.dto.AgencyServiceDTO;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.dto.ServicePatchRequest;
import nova.enterprise_service_hub.dto.ServiceRequest;
import nova.enterprise_service_hub.mapper.AgencyServiceMapper;
import nova.enterprise_service_hub.model.AgencyService;
import nova.enterprise_service_hub.util.StringSanitizer;
import nova.enterprise_service_hub.repository.AgencyServiceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for {@link AgencyService} operations.
 * Returns DTOs via MapStruct — never leaks entities to the controller.
 */
@Service
@Transactional(readOnly = true)
public class AgencyServiceService {

    private final AgencyServiceRepository repository;
    private final AgencyServiceMapper mapper;
    private final AssetService assetService;

    public AgencyServiceService(AgencyServiceRepository repository,
            AgencyServiceMapper mapper,
            AssetService assetService) {
        this.repository = repository;
        this.mapper = mapper;
        this.assetService = assetService;
    }

    @Cacheable(value = "services", key = "'allActive'")
    public List<AgencyServiceDTO> findAllActive() {
        return mapper.toDtoList(repository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc());
    }

    public PageResponse<AgencyServiceDTO> findAllActivePaged(Pageable pageable) {
        return PageResponse.from(repository.findAllByActiveTrue(pageable).map(mapper::toDto));
    }

    public Optional<AgencyServiceDTO> findBySlug(String slug) {
        return repository.findBySlugAndActiveTrue(slug).map(mapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "services", allEntries = true)
    public AgencyServiceDTO create(ServiceRequest request) {
        AgencyService service = new AgencyService();
        service.setTitle(StringSanitizer.stripAll(request.title()));
        service.setSlug(request.slug());
        service.setDescription(StringSanitizer.stripAll(request.description()));
        service.setIconName(StringSanitizer.stripAll(request.iconName()));
        service.setImage(assetService.validateAndBuild(request.image()));
        service.setActive(request.active() != null ? request.active() : true);
        return mapper.toDto(repository.save(service));
    }

    @Transactional
    @CacheEvict(value = "services", allEntries = true)
    public AgencyServiceDTO update(Long id, ServiceRequest request) {
        AgencyService service = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found with id: " + id));
        service.setTitle(StringSanitizer.stripAll(request.title()));
        service.setSlug(request.slug());
        service.setDescription(StringSanitizer.stripAll(request.description()));
        service.setIconName(StringSanitizer.stripAll(request.iconName()));
        service.setImage(assetService.validateAndBuild(request.image()));
        if (request.active() != null) {
            service.setActive(request.active());
        }
        return mapper.toDto(repository.save(service));
    }

    /**
     * PATCH — applies only non-null fields from the request.
     * Enables single-field updates like toggling active or changing displayOrder.
     */
    @Transactional
    @CacheEvict(value = "services", allEntries = true)
    public AgencyServiceDTO patch(Long id, ServicePatchRequest request) {
        AgencyService service = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found with id: " + id));

        if (request.title() != null) {
            service.setTitle(StringSanitizer.stripAll(request.title()));
        }
        if (request.slug() != null) {
            service.setSlug(request.slug());
        }
        if (request.description() != null) {
            service.setDescription(StringSanitizer.stripAll(request.description()));
        }
        if (request.iconName() != null) {
            service.setIconName(StringSanitizer.stripAll(request.iconName()));
        }
        if (request.image() != null) {
            service.setImage(assetService.validateAndBuild(request.image()));
        }
        if (request.displayOrder() != null) {
            service.setDisplayOrder(request.displayOrder());
        }
        if (request.active() != null) {
            service.setActive(request.active());
        }

        return mapper.toDto(repository.save(service));
    }

    @Transactional
    @CacheEvict(value = "services", allEntries = true)
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Service not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
