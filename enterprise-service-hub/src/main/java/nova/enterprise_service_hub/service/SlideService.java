package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.SlideDTO;
import nova.enterprise_service_hub.exception.ResourceNotFoundException;
import nova.enterprise_service_hub.model.Slide;
import nova.enterprise_service_hub.repository.SlideRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SlideService {

    private final SlideRepository repository;

    public SlideService(SlideRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "slides", key = "'all'")
    public List<SlideDTO> getAllSlides() {
        return repository.findAllOrderByDisplayOrderAsc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "slides", allEntries = true)
    public SlideDTO createSlide(SlideDTO dto) {
        Slide slide = new Slide();
        BeanUtils.copyProperties(dto, slide, "id");
        Slide saved = repository.save(slide);
        return convertToDTO(saved);
    }

    @Transactional
    @CacheEvict(value = "slides", allEntries = true)
    public SlideDTO updateSlide(Long id, SlideDTO dto) {
        Slide slide = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slide not found with ID: " + id));

        if (dto.getTitle() != null)
            slide.setTitle(dto.getTitle());
        if (dto.getSubtitle() != null)
            slide.setSubtitle(dto.getSubtitle());
        if (dto.getImageUrl() != null)
            slide.setImageUrl(dto.getImageUrl());
        if (dto.getCtaText() != null)
            slide.setCtaText(dto.getCtaText());
        if (dto.getCtaLink() != null)
            slide.setCtaLink(dto.getCtaLink());
        if (dto.getDisplayOrder() != null)
            slide.setDisplayOrder(dto.getDisplayOrder());

        Slide updated = repository.save(slide);
        return convertToDTO(updated);
    }

    @Transactional
    @CacheEvict(value = "slides", allEntries = true)
    public void deleteSlide(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Slide not found with ID: " + id);
        }
        repository.deleteById(id);
    }

    private SlideDTO convertToDTO(Slide entity) {
        SlideDTO dto = new SlideDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
