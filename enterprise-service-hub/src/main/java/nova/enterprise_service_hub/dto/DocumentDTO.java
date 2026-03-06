package nova.enterprise_service_hub.dto;

import java.time.Instant;

public record DocumentDTO(
        Long id,
        String originalName,
        String fileType,
        Long fileSize,
        String contentType,
        String storageUrl,
        String uploadedBy,
        String description,
        Instant createdAt
) {}
