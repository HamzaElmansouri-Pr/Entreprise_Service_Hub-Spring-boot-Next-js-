package nova.enterprise_service_hub.service;

import jakarta.persistence.EntityNotFoundException;
import nova.enterprise_service_hub.dto.ImageMetadataDTO;
import nova.enterprise_service_hub.dto.PageSectionDTO;
import nova.enterprise_service_hub.dto.PageSectionPatchRequest;
import nova.enterprise_service_hub.model.PageSection;
import nova.enterprise_service_hub.repository.PageSectionRepository;
import nova.enterprise_service_hub.util.StringSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PageSectionService {

    private final PageSectionRepository repository;
    private final AssetService assetService;

    public PageSectionService(PageSectionRepository repository, AssetService assetService) {
        this.repository = repository;
        this.assetService = assetService;
    }

    public List<PageSectionDTO> findByPageName(String pageName) {
        return repository.findAllByPageNameIgnoreCaseOrderByDisplayOrderAsc(pageName)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PageSectionDTO patchSection(Long id, PageSectionPatchRequest request) {
        PageSection section = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Page section not found with id: " + id));

        if (request.title() != null) {
            section.setTitle(StringSanitizer.stripAll(request.title()));
        }
        if (request.description() != null) {
            section.setDescription(StringSanitizer.stripAll(request.description()));
        }
        if (request.displayOrder() != null) {
            section.setDisplayOrder(request.displayOrder());
        }
        if (request.contentData() != null) {
            section.setContentData(new LinkedHashMap<>(request.contentData()));
        }
        if (request.image() != null) {
            section.setImage(assetService.validateAndBuild(request.image()));
        }

        return toDto(repository.save(section));
    }

    private PageSectionDTO toDto(PageSection entity) {
        ImageMetadataDTO image = null;
        if (entity.getImage() != null) {
            image = new ImageMetadataDTO(
                    entity.getImage().getUrl(),
                    entity.getImage().getAltText(),
                    entity.getImage().getWidth(),
                    entity.getImage().getHeight());
        }

        Map<String, Object> contentData = entity.getContentData() != null
                ? new LinkedHashMap<>(entity.getContentData())
                : new LinkedHashMap<>();

        return new PageSectionDTO(
                entity.getId(),
                entity.getPageName(),
                entity.getSectionKey(),
                entity.getTitle(),
                entity.getDescription(),
                contentData,
                entity.getDisplayOrder(),
                image,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
