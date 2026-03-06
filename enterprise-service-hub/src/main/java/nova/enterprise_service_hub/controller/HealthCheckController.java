package nova.enterprise_service_hub.controller;

import nova.enterprise_service_hub.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;

/**
 * Health Check Controller — Verification endpoint that proves:
 * <ul>
 * <li>Spring Boot is running</li>
 * <li>The app can reach the Dockerized PostgreSQL database</li>
 * <li>Virtual Threads are active</li>
 * </ul>
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    private final DataSource dataSource;

    public HealthCheckController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> healthCheck() {
        long start = System.nanoTime();

        String dbStatus;
        String dbProductName;

        try (Connection connection = dataSource.getConnection()) {
            dbProductName = connection.getMetaData().getDatabaseProductName()
                    + " " + connection.getMetaData().getDatabaseProductVersion();
            dbStatus = "CONNECTED";
        } catch (Exception e) {
            dbProductName = "N/A";
            dbStatus = "DISCONNECTED — " + e.getMessage();
        }

        long elapsed = (System.nanoTime() - start) / 1_000_000; // ms

        HealthResponse response = new HealthResponse(
                "UP",
                dbStatus,
                dbProductName,
                Thread.currentThread().isVirtual() ? "ACTIVE" : "DISABLED",
                Thread.currentThread().getName(),
                elapsed + "ms",
                Instant.now().toString());

        return ResponseEntity.ok(response);
    }
}
