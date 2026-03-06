package nova.enterprise_service_hub.controller;

import nova.enterprise_service_hub.dto.DocumentDTO;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.service.DocumentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Document Controller — Phase 2.4
 */
@RestController
@RequestMapping("/v1/documents")
@PreAuthorize("hasRole('ADMIN')")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<DocumentDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());

        PageResponse<DocumentDTO> result;
        if (search != null && !search.isBlank()) {
            result = documentService.search(search.trim(), pageable);
        } else if (type != null && !type.isBlank()) {
            result = documentService.findByType(type, pageable);
        } else {
            result = documentService.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<DocumentDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        DocumentDTO doc = documentService.upload(file, description, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
