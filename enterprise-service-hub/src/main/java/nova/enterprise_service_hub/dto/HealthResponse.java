package nova.enterprise_service_hub.dto;

/**
 * Data Transfer Object for the Health Check response.
 * Uses a Java 21 record for immutability and zero boilerplate.
 */
public record HealthResponse(
        String status,
        String database,
        String databaseVersion,
        String virtualThreads,
        String currentThread,
        String responseTime,
        String timestamp) {
}
