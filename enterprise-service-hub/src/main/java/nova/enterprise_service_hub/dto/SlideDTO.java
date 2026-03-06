package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SlideDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 255, message = "Subtitle cannot exceed 255 characters")
    private String subtitle;

    @Size(max = 255, message = "Image URL cannot exceed 255 characters")
    private String imageUrl;

    @Size(max = 50, message = "CTA Text cannot exceed 50 characters")
    private String ctaText;

    @Size(max = 255, message = "CTA Link cannot exceed 255 characters")
    private String ctaLink;

    @NotNull(message = "Display order is mandatory")
    @Min(value = 1, message = "Display order must be at least 1")
    private Integer displayOrder;
}
