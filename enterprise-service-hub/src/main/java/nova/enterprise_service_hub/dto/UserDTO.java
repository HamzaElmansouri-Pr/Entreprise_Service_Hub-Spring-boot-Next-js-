package nova.enterprise_service_hub.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String fullName;
    private String email;
    private String tenantId;
    private Set<String> roles; // Send back mapped role names (e.g. "ROLE_ADMIN")
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
