package nova.enterprise_service_hub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable image metadata — reusable across entities.
 * <p>
 * Stores the image URL, alt text for accessibility/SEO,
 * and dimensions for frontend layout optimization.
 * Designed to connect to a CDN in a future phase.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageMetadata {

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Column(length = 500)
    private String url;

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    @Column(length = 255)
    private String altText;

    private Integer width;

    private Integer height;

    // Explicit Getters and Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
