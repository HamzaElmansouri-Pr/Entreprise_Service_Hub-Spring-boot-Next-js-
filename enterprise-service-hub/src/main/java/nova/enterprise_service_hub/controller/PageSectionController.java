package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.PageSectionDTO;
import nova.enterprise_service_hub.dto.PageSectionPatchRequest;
import nova.enterprise_service_hub.service.PageSectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/pages")
public class PageSectionController {

    private final PageSectionService pageSectionService;

    public PageSectionController(PageSectionService pageSectionService) {
        this.pageSectionService = pageSectionService;
    }

    @GetMapping("/{pageName}/sections")
    public ResponseEntity<List<PageSectionDTO>> getSectionsByPage(@PathVariable String pageName) {
        return ResponseEntity.ok(pageSectionService.findByPageName(pageName));
    }

    @PatchMapping("/sections/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageSectionDTO> patchSection(
            @PathVariable Long id,
            @Valid @RequestBody PageSectionPatchRequest request) {
        return ResponseEntity.ok(pageSectionService.patchSection(id, request));
    }
}
