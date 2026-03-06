package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Document entity — metadata for uploaded files.
 * <p>
 * Phase 2.4: File / Document Management
 */
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_document_tenant", columnList = "tenant_id"),
        @Index(name = "idx_document_type", columnList = "file_type"),
        @Index(name = "idx_document_uploaded_by", columnList = "uploaded_by")
})
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotBlank
    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 500)
    private String storedName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "storage_url", length = 1000)
    private String storageUrl;

    @Column(name = "uploaded_by", length = 150)
    private String uploadedBy;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
