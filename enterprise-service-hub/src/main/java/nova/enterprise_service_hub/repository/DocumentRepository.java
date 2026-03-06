package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findAllByTenantId(String tenantId, Pageable pageable);

    Page<Document> findAllByTenantIdAndFileType(String tenantId, String fileType, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND " +
           "LOWER(d.originalName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Document> searchByName(@Param("tenantId") String tenantId,
                                @Param("query") String query,
                                Pageable pageable);
}
