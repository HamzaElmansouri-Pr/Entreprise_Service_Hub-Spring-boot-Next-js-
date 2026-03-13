package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nova.enterprise_service_hub.security.TenantAware;
import nova.enterprise_service_hub.security.TenantEntityListener;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a portfolio project delivered by Nova Agency.
 * <p>
 * Includes structured case study, image metadata, display ordering
 * for portfolio curation, soft deletion via {@code archived}, and JPA auditing.
 */
@Entity
@Audited
@EntityListeners({ AuditingEntityListener.class, TenantEntityListener.class })
@Table(name = "projects", indexes = {
                @Index(name = "idx_project_client", columnList = "client_name"),
                @Index(name = "idx_project_order", columnList = "display_order"),
                @Index(name = "idx_project_archived", columnList = "archived"),
                @Index(name = "idx_project_tenant", columnList = "tenant_id")
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class Project implements TenantAware {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NotBlank(message = "Project name is required")
        @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
        @Column(nullable = false, length = 150)
        private String name;

        @NotBlank(message = "Client name is required")
        @Size(min = 2, max = 150, message = "Client name must be between 2 and 150 characters")
        @Column(name = "client_name", nullable = false, length = 150)
        private String clientName;

        @NotBlank(message = "Tenant ID is required")
        @Column(name = "tenant_id", length = 50)
        private String tenantId;

        @Size(max = 500)
        @Column(name = "preview_url", length = 500)
        private String previewUrl;

        // ── Structured Case Study Fields ─────────────────────────────────────

        @Size(max = 5000)
        @Column(name = "case_study_challenge", columnDefinition = "TEXT")
        private String caseStudyChallenge;

        @Size(max = 5000)
        @Column(name = "case_study_solution", columnDefinition = "TEXT")
        private String caseStudySolution;

        @Size(max = 5000)
        @Column(name = "case_study_result", columnDefinition = "TEXT")
        private String caseStudyResult;

        // ── Structured Image Metadata ────────────────────────────────────────

        @Embedded
        @AttributeOverrides({
                        @AttributeOverride(name = "url", column = @Column(name = "proj_img_url", length = 500)),
                        @AttributeOverride(name = "altText", column = @Column(name = "proj_img_alt", length = 255)),
                        @AttributeOverride(name = "width", column = @Column(name = "proj_img_width")),
                        @AttributeOverride(name = "height", column = @Column(name = "proj_img_height"))
        })
        private ImageMetadata image;

        @NotAudited
        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "project_gallery", joinColumns = @JoinColumn(name = "project_id"))
        private List<ImageMetadata> gallery = new ArrayList<>();

        // ── Display & Lifecycle ──────────────────────────────────────────────

        @Column(name = "display_order", nullable = false)
        private Integer displayOrder = 0;

        @Column(nullable = false)
        private boolean archived = false;

        @ElementCollection(fetch = FetchType.LAZY)
        @CollectionTable(name = "project_technologies", joinColumns = @JoinColumn(name = "project_id"))
        @Column(name = "technology", length = 80)
        private List<String> technologies = new ArrayList<>();

        @NotAudited
        @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("timestamp DESC")
        private List<ProjectUpdate> updates = new ArrayList<>();

        @NotAudited
        @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("uploadedAt DESC")
        private List<ProjectFile> files = new ArrayList<>();

        @CreatedDate
        @Column(name = "created_at", updatable = false)
        private Instant createdAt;

        @LastModifiedDate
        @Column(name = "updated_at")
        private Instant updatedAt;
}
