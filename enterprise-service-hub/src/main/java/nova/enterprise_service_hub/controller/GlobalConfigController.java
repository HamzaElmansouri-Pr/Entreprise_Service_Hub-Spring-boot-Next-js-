package nova.enterprise_service_hub.controller;

import jakarta.validation.Valid;
import nova.enterprise_service_hub.dto.GlobalConfigDTO;
import nova.enterprise_service_hub.service.GlobalConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/config")
public class GlobalConfigController {

    private final GlobalConfigService service;

    public GlobalConfigController(GlobalConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<GlobalConfigDTO> getConfig() {
        return ResponseEntity.ok(service.getConfig());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalConfigDTO> updateConfig(@Valid @RequestBody GlobalConfigDTO configDTO) {
        return ResponseEntity.ok(service.updateConfig(configDTO));
    }
}
