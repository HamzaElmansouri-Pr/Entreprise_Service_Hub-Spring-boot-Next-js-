package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.model.ApiKey;
import nova.enterprise_service_hub.repository.ApiKeyRepository;
import nova.enterprise_service_hub.security.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * API Key Service — Phase 6
 * <p>
 * Generates, validates, and manages API keys for B2B integrations.
 */
@Service
public class ApiKeyService {

    private final ApiKeyRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Generate a new API key. Returns the raw key only once — it is
     * stored as a BCrypt hash.
     */
    @Transactional
    public String generateApiKey(String name) {
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        String rawKey = "esh_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);

        ApiKey apiKey = new ApiKey();
        apiKey.setTenantId(TenantContext.getTenantId());
        apiKey.setKeyHash(passwordEncoder.encode(rawKey));
        apiKey.setKeyPrefix(rawKey.substring(0, 8));
        apiKey.setName(name);
        apiKey.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
        repository.save(apiKey);

        return rawKey;
    }

    /**
     * Validate a raw API key against stored hashes.
     * Returns the tenant ID if valid, null otherwise.
     */
    public String validateApiKey(String rawKey) {
        // Search by prefix for efficiency, then verify hash
        List<ApiKey> candidates = repository.findAll();
        for (ApiKey key : candidates) {
            if (!key.isActive()) continue;
            if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(Instant.now())) continue;
            if (passwordEncoder.matches(rawKey, key.getKeyHash())) {
                return key.getTenantId();
            }
        }
        return null;
    }

    public List<ApiKey> getKeysForCurrentTenant() {
        return repository.findByTenantId(TenantContext.getTenantId());
    }

    @Transactional
    public void revokeKey(Long id) {
        ApiKey key = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("API key not found: " + id));
        key.setActive(false);
        repository.save(key);
    }
}
