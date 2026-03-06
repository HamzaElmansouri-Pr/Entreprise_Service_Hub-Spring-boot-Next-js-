package nova.enterprise_service_hub.controller;

import nova.enterprise_service_hub.dto.AnalyticsDTO;
import nova.enterprise_service_hub.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytics Controller — Phase 2.5
 * <p>
 * Exposes dashboard analytics and reporting endpoints.
 */
@RestController
@RequestMapping("/v1/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDTO.DashboardSummary> getDashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardSummary());
    }

    @GetMapping("/projects")
    public ResponseEntity<AnalyticsDTO.ProjectAnalytics> getProjectAnalytics() {
        return ResponseEntity.ok(analyticsService.getProjectAnalytics());
    }
}
