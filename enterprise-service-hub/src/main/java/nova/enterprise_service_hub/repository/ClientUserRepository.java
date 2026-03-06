package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.ClientUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientUserRepository extends JpaRepository<ClientUser, Long> {

    Optional<ClientUser> findByEmail(String email);

    Optional<ClientUser> findByInviteToken(String inviteToken);

    List<ClientUser> findByTenantId(String tenantId);

    boolean existsByEmail(String email);
}
