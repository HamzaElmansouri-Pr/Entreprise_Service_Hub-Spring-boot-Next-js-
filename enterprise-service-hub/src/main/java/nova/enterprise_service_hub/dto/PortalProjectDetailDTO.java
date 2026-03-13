package nova.enterprise_service_hub.dto;

import java.time.Instant;
import java.util.List;

public record PortalProjectDetailDTO(
        Long id,
        String name,
        String status,
        int progressPercent,
        String description,
        List<ProjectUpdateDTO> updates,
        List<ProjectFileDTO> files) {

    public record ProjectUpdateDTO(
            Long id,
            String title, // "Sprint 1 Completed"
            String detail, // "We finished X, Y, Z"
            Instant timestamp) {
    }

    public record ProjectFileDTO(
            Long id,
            String fileName,
            String downloadUrl,
            String fileType,
            long sizeBytes,
            Instant uploadedAt) {
    }
}
