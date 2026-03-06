package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.DocumentDTO;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.model.Document;
import nova.enterprise_service_hub.repository.DocumentRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Document Service — Phase 2.4
 * <p>
 * Handles file upload, metadata persistence, and retrieval.
 * Stores files on local filesystem (can be swapped for S3/MinIO).
 */
@Service
@Transactional(readOnly = true)
public class DocumentService {

    private static final Path UPLOAD_DIR = Paths.get("uploads", "documents");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".csv",
            ".png", ".jpg", ".jpeg", ".webp", ".svg",
            ".txt", ".md", ".json", ".zip"
    );
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB

    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public PageResponse<DocumentDTO> findAll(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        return PageResponse.from(repository.findAllByTenantId(tenantId, pageable).map(this::toDTO));
    }

    public PageResponse<DocumentDTO> findByType(String fileType, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        return PageResponse.from(
                repository.findAllByTenantIdAndFileType(tenantId, fileType, pageable).map(this::toDTO));
    }

    public PageResponse<DocumentDTO> search(String query, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        return PageResponse.from(repository.searchByName(tenantId, query, pageable).map(this::toDTO));
    }

    @Transactional
    public DocumentDTO upload(MultipartFile file, String description, String uploadedBy) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum size of 25MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase()
                : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed: " + extension);
        }

        String storedName = UUID.randomUUID() + extension;
        Path tenantDir = UPLOAD_DIR.resolve(TenantContext.getTenantId());
        Files.createDirectories(tenantDir);
        Path filePath = tenantDir.resolve(storedName);
        file.transferTo(filePath.toFile());

        Document doc = new Document();
        doc.setTenantId(TenantContext.getTenantId());
        doc.setOriginalName(originalName);
        doc.setStoredName(storedName);
        doc.setFileType(getFileCategory(extension));
        doc.setFileSize(file.getSize());
        doc.setContentType(file.getContentType());
        doc.setStorageUrl("/uploads/documents/" + TenantContext.getTenantId() + "/" + storedName);
        doc.setUploadedBy(uploadedBy);
        doc.setDescription(description);

        return toDTO(repository.save(doc));
    }

    @Transactional
    public void delete(Long id) {
        Document doc = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + id));
        // Delete physical file
        try {
            Path filePath = UPLOAD_DIR.resolve(doc.getTenantId()).resolve(doc.getStoredName());
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
        repository.delete(doc);
    }

    private String getFileCategory(String extension) {
        return switch (extension) {
            case ".pdf" -> "PDF";
            case ".doc", ".docx" -> "Word";
            case ".xls", ".xlsx", ".csv" -> "Spreadsheet";
            case ".png", ".jpg", ".jpeg", ".webp", ".svg" -> "Image";
            case ".zip" -> "Archive";
            default -> "Other";
        };
    }

    private DocumentDTO toDTO(Document d) {
        return new DocumentDTO(d.getId(), d.getOriginalName(), d.getFileType(),
                d.getFileSize(), d.getContentType(), d.getStorageUrl(),
                d.getUploadedBy(), d.getDescription(), d.getCreatedAt());
    }
}
