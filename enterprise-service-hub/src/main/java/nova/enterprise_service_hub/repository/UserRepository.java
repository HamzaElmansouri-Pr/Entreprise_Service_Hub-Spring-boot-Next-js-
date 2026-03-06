package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByCreatedAtDesc();

    long countByRoles_NameAndEnabledTrue(String roleName);

    long countByTenantId(String tenantId);
}
