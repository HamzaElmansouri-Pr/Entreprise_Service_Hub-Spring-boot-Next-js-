package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.ImageMetadataDTO;
import nova.enterprise_service_hub.model.ImageMetadata;
import nova.enterprise_service_hub.util.StringSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Asset Service — validates image metadata and handles file storage.
 * <p>
 * Enforces allowed file extensions (OWASP-aligned) and sanitizes alt text.
 * Stores uploaded files in the local "uploads" directory.
 */
@Service
public class AssetService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".webp", ".png", ".jpg", ".jpeg", ".svg");

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".webm", ".mov", ".ogg");

    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();

    /**
     * Validates image metadata and converts DTO → entity embeddable.
     *
     * @param dto the image metadata from the API request
     * @return a validated {@link ImageMetadata} ready for persistence
     * @throws IllegalArgumentException if the image URL has a disallowed extension
     */
    public ImageMetadata validateAndBuild(ImageMetadataDTO dto) {
        if (dto == null || dto.url() == null || dto.url().isBlank()) {
            return null;
        }

        String url = dto.url().trim().toLowerCase();

        // Skip extension check for local upload paths (already validated at upload time)
        if (!url.startsWith("/uploads/")) {
            boolean isImage = ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(url::endsWith);
            boolean isVideo = ALLOWED_VIDEO_EXTENSIONS.stream().anyMatch(url::endsWith);
            
            if (!isImage && !isVideo) {
                throw new IllegalArgumentException(
                        "Invalid file extension. Allowed images: " + ALLOWED_IMAGE_EXTENSIONS + 
                        ", Allowed videos: " + ALLOWED_VIDEO_EXTENSIONS);
            }
        }

        ImageMetadata metadata = new ImageMetadata(
                dto.url().trim(),
                StringSanitizer.stripAll(dto.altText()),
                dto.width(),
                dto.height(),
                dto.thumbnailUrl() != null ? dto.thumbnailUrl().trim() : null);
        return metadata;
    }

    /**
     * Stores an uploaded image file to the local uploads directory.
     *
     * @param file the uploaded multipart file
     * @return the relative URL path to the stored file (e.g., /uploads/abc123.png)
     * @throws IllegalArgumentException if the file type is not allowed or is empty
     */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name is missing");
        }

        // Validate extension
        String lowerName = originalFilename.toLowerCase();
        boolean validExtension = ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExtension) {
            throw new IllegalArgumentException(
                    "Invalid image extension. Allowed: " + ALLOWED_IMAGE_EXTENSIONS);
        }

        // Extract extension and generate unique filename
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String uniqueName = UUID.randomUUID().toString() + ext;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path target = UPLOAD_DIR.resolve(uniqueName).normalize();

            // Security: ensure the target is inside the upload directory
            if (!target.startsWith(UPLOAD_DIR)) {
                throw new IllegalArgumentException("Invalid file path");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Return a URL path relative to the API base
            return "/uploads/" + uniqueName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image: " + e.getMessage(), e);
        }
    }
    /**
     * Stores an uploaded video file to the local uploads directory.
     *
     * @param file the uploaded multipart file
     * @return the relative URL path to the stored file
     */
    public String storeVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name is missing");
        }

        String lowerName = originalFilename.toLowerCase();
        boolean validExtension = ALLOWED_VIDEO_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExtension) {
            throw new IllegalArgumentException(
                    "Invalid video extension. Allowed: " + ALLOWED_VIDEO_EXTENSIONS);
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String uniqueName = UUID.randomUUID().toString() + ext;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path target = UPLOAD_DIR.resolve(uniqueName).normalize();

            if (!target.startsWith(UPLOAD_DIR)) {
                throw new IllegalArgumentException("Invalid file path");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + uniqueName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store video: " + e.getMessage(), e);
        }
    }
}
