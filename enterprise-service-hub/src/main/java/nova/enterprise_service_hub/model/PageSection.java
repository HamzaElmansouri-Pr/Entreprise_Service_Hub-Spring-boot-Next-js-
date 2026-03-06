package nova.enterprise_service_hub.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nova.enterprise_service_hub.security.TenantAware;
import nova.enterprise_service_hub.security.TenantEntityListener;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ParamDef;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@EntityListeners({ AuditingEntityListener.class, TenantEntityListener.class })
@Table(name = "page_sections", indexes = {
                @Index(name = "idx_page_sections_page", columnList = "page_name"),
                @Index(name = "idx_page_sections_order", columnList = "display_order"),
                @Index(name = "idx_page_sections_key", columnList = "section_key"),
                @Index(name = "idx_page_sections_tenant", columnList = "tenant_id")
})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class PageSection implements TenantAware {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NotBlank(message = "Page name is required")
        @Size(max = 80, message = "Page name cannot exceed 80 characters")
        @Column(name = "page_name", nullable = false, length = 80)
        private String pageName;

        @NotBlank(message = "Section key is required")
        @Size(max = 120, message = "Section key cannot exceed 120 characters")
        @Column(name = "section_key", nullable = false, length = 120)
        private String sectionKey;

        @Size(max = 180, message = "Title cannot exceed 180 characters")
        @Column(length = 180)
        private String title;

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        @Column(length = 2000)
        private String description;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "content_data", columnDefinition = "jsonb")
        private Map<String, Object> contentData = new LinkedHashMap<>();

        @Min(value = 1, message = "Display order must be at least 1")
        @Column(name = "display_order", nullable = false)
        private Integer displayOrder = 1;

        @Embedded
        @AttributeOverrides({
                        @AttributeOverride(name = "url", column = @Column(name = "section_img_url", length = 500)),
                        @AttributeOverride(name = "altText", column = @Column(name = "section_img_alt", length = 255)),
                        @AttributeOverride(name = "width", column = @Column(name = "section_img_width")),
                        @AttributeOverride(name = "height", column = @Column(name = "section_img_height"))
        })
        private ImageMetadata image;

        @NotBlank(message = "Tenant ID is required")
        @Column(name = "tenant_id", length = 50)
        private String tenantId;

        @CreatedDate
        @Column(name = "created_at", updatable = false)
        private Instant createdAt;

        @LastModifiedDate
        @Column(name = "updated_at")
        private Instant updatedAt;

        // Explicit Getters and Setters
        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public String getPageName() {
                return pageName;
        }

        public void setPageName(String pageName) {
                this.pageName = pageName;
        }

        public String getSectionKey() {
                return sectionKey;
        }

        public void setSectionKey(String sectionKey) {
                this.sectionKey = sectionKey;
        }

        public String getTitle() {
                return title;
        }

        public void setTitle(String title) {
                this.title = title;
        }

        public String getDescription() {
                return description;
        }

        public void setDescription(String description) {
                this.description = description;
        }

        public Map<String, Object> getContentData() {
                return contentData;
        }

        public void setContentData(Map<String, Object> contentData) {
                this.contentData = contentData;
        }

        public Integer getDisplayOrder() {
                return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
                this.displayOrder = displayOrder;
        }

        public ImageMetadata getImage() {
                return image;
        }

        public void setImage(ImageMetadata image) {
                this.image = image;
        }

        public String getTenantId() {
                return tenantId;
        }

        public void setTenantId(String tenantId) {
                this.tenantId = tenantId;
        }

        public Instant getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
                this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
                return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
                this.updatedAt = updatedAt;
        }
}
