package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Page<Invoice> findByTenantId(String tenantId, Pageable pageable);

    Optional<Invoice> findByIdAndTenantId(Long id, String tenantId);

    Optional<Invoice> findByReferenceNumber(String referenceNumber);

    List<Invoice> findByProjectId(Long projectId);

    List<Invoice> findByTenantIdAndStatus(String tenantId, Invoice.InvoiceStatus status);

    long countByTenantId(String tenantId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status = 'PAID'")
    java.math.BigDecimal sumPaidByTenantId(@Param("tenantId") String tenantId);
}
