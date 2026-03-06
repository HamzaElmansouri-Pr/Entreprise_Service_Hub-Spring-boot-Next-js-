package nova.enterprise_service_hub.controller;

import nova.enterprise_service_hub.service.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Upload Controller — handles file uploads for images.
 * Files are stored locally in the /uploads directory and served statically.
 */
@RestController
@RequestMapping("/v1/uploads")
public class UploadController {

    private final AssetService assetService;

    public UploadController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping("/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = assetService.storeImage(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
