package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);

    List<ApiKey> findByTenantId(String tenantId);
}
