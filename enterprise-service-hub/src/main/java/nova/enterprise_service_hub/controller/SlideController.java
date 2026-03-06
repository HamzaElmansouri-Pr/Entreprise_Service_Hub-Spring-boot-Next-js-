package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.SlideDTO;
import nova.enterprise_service_hub.service.SlideService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/slides")
public class SlideController {

    private final SlideService service;

    public SlideController(SlideService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SlideDTO>> getAllSlides() {
        return ResponseEntity.ok(service.getAllSlides());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlideDTO> createSlide(@Valid @RequestBody SlideDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSlide(dto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlideDTO> updateSlide(
            @PathVariable Long id,
            @RequestBody SlideDTO dto) { // @RequestBody might not be fully @Valid for PATCH since it's partial
        return ResponseEntity.ok(service.updateSlide(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSlide(@PathVariable Long id) {
        service.deleteSlide(id);
        return ResponseEntity.noContent().build();
    }
}
