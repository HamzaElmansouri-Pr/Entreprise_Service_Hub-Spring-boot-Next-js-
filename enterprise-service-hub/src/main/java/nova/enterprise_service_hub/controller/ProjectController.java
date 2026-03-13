package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.dto.ProjectDTO;
import nova.enterprise_service_hub.dto.ProjectPatchRequest;
import nova.enterprise_service_hub.dto.ProjectRequest;
import nova.enterprise_service_hub.service.ProjectService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Portfolio Projects.
 * <p>
 * GET endpoints are public for portfolio showcase.
 * POST/PUT/PATCH/DELETE are restricted to ROLE_ADMIN.
 * DELETE performs soft-deletion (archived = true).
 */
@RestController
@RequestMapping("/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<ProjectDTO>> getProjectsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String[] sortParts = sort.split(",");
        Sort sortObj = Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "desc"), sortParts[0]);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sortObj);
        return ResponseEntity.ok(projectService.findAllPaged(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @GetMapping("/tech/{technology}")
    public ResponseEntity<List<ProjectDTO>> getProjectsByTechnology(@PathVariable String technology) {
        return ResponseEntity.ok(projectService.findByTechnology(technology));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<ProjectDTO>> filterProjectsByTechnology(@RequestParam String tech) {
        return ResponseEntity.ok(projectService.findByTechnology(tech));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProjectDTO>> searchProjects(@RequestParam String q) {
        return ResponseEntity.ok(projectService.search(q));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<ProjectDTO> updateProject(@PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    /**
     * PATCH /api/v1/projects/{id} — Partial update (ADMIN ONLY).
     * Archive, reorder, or update individual fields.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<ProjectDTO> patchProject(@PathVariable Long id,
            @RequestBody ProjectPatchRequest request) {
        return ResponseEntity.ok(projectService.patch(id, request));
    }

    /**
     * DELETE /api/v1/projects/{id} — Soft-delete (ADMIN ONLY).
     * Sets archived = true instead of removing from the database.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
