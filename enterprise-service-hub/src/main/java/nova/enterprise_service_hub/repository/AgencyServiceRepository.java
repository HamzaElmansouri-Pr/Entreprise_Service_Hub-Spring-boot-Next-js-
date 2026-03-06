package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.AgencyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link AgencyService} entity.
 * <p>
 * Default ordering: displayOrder ASC, createdAt DESC.
 */
@Repository
public interface AgencyServiceRepository extends JpaRepository<AgencyService, Long> {

    Optional<AgencyService> findBySlugAndActiveTrue(String slug);

    List<AgencyService> findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();

    Page<AgencyService> findAllByActiveTrue(Pageable pageable);
}
