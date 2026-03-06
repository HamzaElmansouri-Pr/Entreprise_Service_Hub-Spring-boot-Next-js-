package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class GlobalConfigDTO {
    private Long id;

    @NotBlank(message = "Agency name is required")
    @Size(max = 100, message = "Agency name cannot exceed 100 characters")
    private String agencyName;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String contactEmail;

    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    private String contactPhone;

    @URL(message = "Must be a valid URL")
    @Size(max = 255, message = "URL cannot exceed 255 characters")
    private String linkedInUrl;

    @URL(message = "Must be a valid URL")
    @Size(max = 255, message = "URL cannot exceed 255 characters")
    private String twitterUrl;

    @URL(message = "Must be a valid URL")
    @Size(max = 255, message = "URL cannot exceed 255 characters")
    private String logoUrl;
}
