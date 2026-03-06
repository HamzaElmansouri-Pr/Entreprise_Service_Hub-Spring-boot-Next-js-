package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.GlobalConfigDTO;
import nova.enterprise_service_hub.exception.ResourceNotFoundException;
import nova.enterprise_service_hub.model.GlobalConfig;
import nova.enterprise_service_hub.repository.GlobalConfigRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalConfigService {

    private final GlobalConfigRepository repository;

    public GlobalConfigService(GlobalConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets the active global configuration. Assumes there is exactly 1 row (ID=1).
     */
    @Cacheable(value = "globalConfig", key = "'singleton'")
    public GlobalConfigDTO getConfig() {
        GlobalConfig config = repository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Global Configuration not found (ID 1 missing)"));
        return convertToDTO(config);
    }

    /**
     * Updates the global configuration.
     */
    @Transactional
    @CacheEvict(value = "globalConfig", allEntries = true)
    public GlobalConfigDTO updateConfig(GlobalConfigDTO dto) {
        GlobalConfig config = repository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Global Configuration not found (ID 1 missing)"));

        if (dto.getAgencyName() != null)
            config.setAgencyName(dto.getAgencyName());
        if (dto.getContactEmail() != null)
            config.setContactEmail(dto.getContactEmail());
        if (dto.getContactPhone() != null)
            config.setContactPhone(dto.getContactPhone());
        if (dto.getLinkedInUrl() != null)
            config.setLinkedInUrl(dto.getLinkedInUrl());
        if (dto.getTwitterUrl() != null)
            config.setTwitterUrl(dto.getTwitterUrl());
        if (dto.getLogoUrl() != null)
            config.setLogoUrl(dto.getLogoUrl());

        GlobalConfig updated = repository.save(config);
        return convertToDTO(updated);
    }

    private GlobalConfigDTO convertToDTO(GlobalConfig entity) {
        GlobalConfigDTO dto = new GlobalConfigDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
