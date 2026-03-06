package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.GlobalConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalConfigRepository extends JpaRepository<GlobalConfig, Long> {
}
