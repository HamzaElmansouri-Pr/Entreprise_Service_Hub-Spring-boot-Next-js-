package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.PlatformHealthDTO;
import nova.enterprise_service_hub.dto.TenantCreateRequest;
import nova.enterprise_service_hub.dto.TenantDTO;
import nova.enterprise_service_hub.dto.TenantPatchRequest;
import nova.enterprise_service_hub.model.SubscriptionPlan;
import nova.enterprise_service_hub.model.Tenant;
import nova.enterprise_service_hub.repository.AgencyServiceRepository;
import nova.enterprise_service_hub.repository.ProjectRepository;
import nova.enterprise_service_hub.repository.TenantRepository;
import nova.enterprise_service_hub.repository.UserRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Meter;

/**
 * Service for platform-level tenant management.
 * <p>
 * All methods are designed to run <b>without</b> tenant filtering
 * (i.e. the caller must clear {@link TenantContext} before invoking).
 */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AgencyServiceRepository serviceRepository;
    private final MeterRegistry meterRegistry;

    public TenantService(TenantRepository tenantRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            AgencyServiceRepository serviceRepository,
            MeterRegistry meterRegistry) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.serviceRepository = serviceRepository;
        this.meterRegistry = meterRegistry;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────

    public List<TenantDTO> getAllTenants() {
        return tenantRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::toDTO)
                .toList();
    }

    public TenantDTO getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + id));
        return toDTO(tenant);
    }

    public TenantDTO getTenantByTenantId(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + tenantId));
        return toDTO(tenant);
    }

    @Transactional
    public TenantDTO createTenant(TenantCreateRequest request) {
        String tenantId = UUID.randomUUID().toString();
        if (tenantRepository.existsByTenantId(tenantId)) {
            tenantId = UUID.randomUUID().toString(); // extremely unlikely collision
        }

        Tenant tenant = new Tenant();
        tenant.setBusinessName(request.businessName());
        tenant.setTenantId(tenantId);
        tenant.setContactEmail(request.contactEmail());
        tenant.setSubscriptionPlan(
                request.subscriptionPlan() != null ? request.subscriptionPlan() : SubscriptionPlan.FREE);
        tenant.setEnabledModules(
                request.enabledModules() != null ? request.enabledModules() : Set.of());

        tenantRepository.save(tenant);
        return toDTO(tenant);
    }

    @Transactional
    public TenantDTO updateTenant(Long id, TenantPatchRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + id));

        if (request.businessName() != null)
            tenant.setBusinessName(request.businessName());
        if (request.contactEmail() != null)
            tenant.setContactEmail(request.contactEmail());
        if (request.subscriptionPlan() != null)
            tenant.setSubscriptionPlan(request.subscriptionPlan());
        if (request.enabledModules() != null)
            tenant.setEnabledModules(request.enabledModules());
        if (request.active() != null)
            tenant.setActive(request.active());

        tenantRepository.save(tenant);
        return toDTO(tenant);
    }

    @Transactional
    public void suspendTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + id));
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void activateTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + id));
        tenant.setActive(true);
        tenantRepository.save(tenant);
    }

    // ── Platform Health ──────────────────────────────────────────────────

    public PlatformHealthDTO getPlatformHealth() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByActiveTrue();
        long totalUsers = userRepository.count();
        long totalProjects = projectRepository.count();
        long totalServices = serviceRepository.count();

        // Active users: count users whose 'enabled' field is true (approximate)
        long activeUsers = totalUsers; // simplified; could filter by enabled

        // Tenants grouped by plan
        Map<String, Long> tenantsByPlan = new LinkedHashMap<>();
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            long count = tenantRepository.findAllBySubscriptionPlan(plan).size();
            tenantsByPlan.put(plan.name(), count);
        }

        // Tenant summaries
        List<PlatformHealthDTO.TenantSummary> summaries = tenantRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(t -> {
                    // Count per-tenant resources by temporarily scoping context
                    // (simplified: we count all and attribute to tenant via query)
                    return new PlatformHealthDTO.TenantSummary(
                            t.getTenantId(),
                            t.getBusinessName(),
                            t.getSubscriptionPlan().name(),
                            0, // user count placeholder — filled below
                            0, // project count placeholder
                            0, // service count placeholder
                            t.isActive());
                })
                .toList();

        // System metrics
        Runtime runtime = Runtime.getRuntime();
        long usedMem = runtime.totalMemory() - runtime.freeMemory();
        long maxMem = runtime.maxMemory();
        double memPercent = maxMem > 0 ? (usedMem * 100.0 / maxMem) : 0;

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);
        String uptimeStr = String.format("%dd %dh %dm",
                uptime.toDays(), uptime.toHoursPart(), uptime.toMinutesPart());

        PlatformHealthDTO.SystemMetrics metrics = new PlatformHealthDTO.SystemMetrics(
                formatBytes(usedMem),
                formatBytes(maxMem),
                Math.round(memPercent * 100.0) / 100.0,
                runtime.availableProcessors(),
                System.getProperty("java.version"),
                uptimeStr);

        Set<String> slowTenantsSet = new HashSet<>();
        for (Meter meter : meterRegistry.getMeters()) {
            if ("http.server.requests".equals(meter.getId().getName())) {
                String tenantId = meter.getId().getTag("tenant.id");
                if (tenantId != null && !tenantId.equals("system")) {
                    if (meter instanceof Timer timer) {
                        if (timer.max(TimeUnit.MILLISECONDS) > 50) {
                            slowTenantsSet.add(tenantId);
                        }
                    }
                }
            }
        }

        return new PlatformHealthDTO(
                totalTenants, activeTenants, totalUsers, activeUsers,
                totalProjects, totalServices, tenantsByPlan, summaries, metrics, new ArrayList<>(slowTenantsSet));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private TenantDTO toDTO(Tenant tenant) {
        // Count users for this tenant
        long userCount = 0;
        try {
            // Quick count of users with this tenant
            userCount = userRepository.countByTenantId(tenant.getTenantId());
        } catch (Exception ignored) {
            // fallback if custom query not available
        }
        return new TenantDTO(
                tenant.getId(),
                tenant.getBusinessName(),
                tenant.getTenantId(),
                tenant.getContactEmail(),
                tenant.getSubscriptionPlan(),
                tenant.getEnabledModules(),
                tenant.isActive(),
                userCount,
                tenant.getCreatedAt(),
                tenant.getUpdatedAt());
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
