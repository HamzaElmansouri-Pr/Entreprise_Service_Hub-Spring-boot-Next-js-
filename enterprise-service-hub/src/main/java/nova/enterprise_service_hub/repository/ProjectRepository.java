package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Project} entity.
 * <p>
 * Default ordering: displayOrder ASC, createdAt DESC.
 * Non-archived projects only for public queries.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByClientNameIgnoreCase(String clientName);

    @Query("SELECT DISTINCT p FROM Project p JOIN p.technologies t WHERE LOWER(t) = LOWER(:tech) AND p.archived = false ORDER BY p.displayOrder ASC")
    List<Project> findByTechnology(@Param("tech") String technology);

    List<Project> findAllByArchivedFalseOrderByDisplayOrderAscCreatedAtDesc();

    Page<Project> findAllByArchivedFalse(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.technologies t " +
            "WHERE p.archived = false AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.clientName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(t) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.displayOrder ASC")
    List<Project> searchByQuery(@Param("query") String query);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.technologies t " +
            "WHERE p.archived = false AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.clientName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(t) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Project> searchByQuery(@Param("query") String query, Pageable pageable);

    long countByArchivedFalse();
}
