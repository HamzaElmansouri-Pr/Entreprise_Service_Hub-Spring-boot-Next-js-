package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

import java.time.Instant;

/**
 * Represents a service offered by Nova Agency.
 * <p>
 * Maps to the {@code agency_services} table. Each service has an SEO-friendly
 * slug, a Lucide-react icon, structured image metadata, display ordering
 * for portfolio curation, and JPA audit timestamps.
 */
@Entity
@Audited
@EntityListeners({ AuditingEntityListener.class, TenantEntityListener.class })
@Table(name = "agency_services", indexes = {
                @Index(name = "idx_service_slug", columnList = "slug", unique = true),
                @Index(name = "idx_service_active", columnList = "active"),
                @Index(name = "idx_service_order", columnList = "display_order"),
                @Index(name = "idx_service_tenant", columnList = "tenant_id")
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class AgencyService implements TenantAware {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NotBlank(message = "Service title is required")
        @Size(min = 2, max = 120, message = "Title must be between 2 and 120 characters")
        @Column(nullable = false, length = 120)
        private String title;

        @NotBlank(message = "SEO slug is required")
        @Size(min = 2, max = 140, message = "Slug must be between 2 and 140 characters")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens")
        @Column(nullable = false, unique = true, length = 140)
        private String slug;

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
        @Column(nullable = false, length = 2000)
        private String description;

        @Size(max = 60, message = "Icon name must not exceed 60 characters")
        @Column(name = "icon_name", length = 60)
        private String iconName;

        @Embedded
        @AttributeOverrides({
                        @AttributeOverride(name = "url", column = @Column(name = "svc_img_url", length = 500)),
                        @AttributeOverride(name = "altText", column = @Column(name = "svc_img_alt", length = 255)),
                        @AttributeOverride(name = "width", column = @Column(name = "svc_img_width")),
                        @AttributeOverride(name = "height", column = @Column(name = "svc_img_height"))
        })
        private ImageMetadata image;

        @Column(name = "display_order", nullable = false)
        private Integer displayOrder = 0;

        @Column(nullable = false)
        private boolean active = true;

        @NotBlank(message = "Tenant ID is required")
        @Column(name = "tenant_id", length = 50)
        private String tenantId;

        @CreatedDate
        @Column(name = "created_at", updatable = false)
        private Instant createdAt;

        @LastModifiedDate
        @Column(name = "updated_at")
        private Instant updatedAt;
}
