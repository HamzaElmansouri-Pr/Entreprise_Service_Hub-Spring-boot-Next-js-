package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.AnalyticsDTO;
import nova.enterprise_service_hub.model.Invoice;
import nova.enterprise_service_hub.model.Project;
import nova.enterprise_service_hub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service — Phase 2.5
 * <p>
 * Aggregates data across invoices, projects, users, and services
 * for dashboard visualizations and reporting.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ProjectRepository projectRepository;
    private final AgencyServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public AnalyticsService(ProjectRepository projectRepository,
                            AgencyServiceRepository serviceRepository,
                            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    public AnalyticsDTO.ProjectAnalytics getProjectAnalytics() {
        List<Project> allProjects = projectRepository.findAll();
        long total = allProjects.size();
        long active = allProjects.stream().filter(p -> !p.isArchived()).count();
        long archived = total - active;
        double completionRate = total > 0 ? (archived * 100.0 / total) : 0;

        Map<String, Long> byTech = new LinkedHashMap<>();
        for (Project p : allProjects) {
            if (p.getTechnologies() != null) {
                for (String tech : p.getTechnologies()) {
                    byTech.merge(tech, 1L, Long::sum);
                }
            }
        }

        return new AnalyticsDTO.ProjectAnalytics(total, active, archived, completionRate, byTech);
    }

    public AnalyticsDTO.DashboardSummary getDashboardSummary() {
        AnalyticsDTO.ProjectAnalytics projects = getProjectAnalytics();

        long totalServices = serviceRepository.count();
        long activeServices = serviceRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc().size();

        long totalUsers = userRepository.count();
        AnalyticsDTO.UserAnalytics users = new AnalyticsDTO.UserAnalytics(
                totalUsers, totalUsers, Map.of());

        // Revenue placeholder — requires Invoice queries when fully wired
        AnalyticsDTO.RevenueAnalytics revenue = new AnalyticsDTO.RevenueAnalytics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());

        return new AnalyticsDTO.DashboardSummary(revenue, projects, users, totalServices, activeServices);
    }
}
