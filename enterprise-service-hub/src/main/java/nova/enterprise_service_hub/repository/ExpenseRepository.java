package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByTenantIdOrderByExpenseDateDesc(String tenantId);

    Page<Expense> findByTenantId(String tenantId, Pageable pageable);

    Optional<Expense> findByIdAndTenantId(Long id, String tenantId);

    List<Expense> findByTenantIdAndCategory(String tenantId, Expense.ExpenseCategory category);

    List<Expense> findByTenantIdAndExpenseDateBetween(String tenantId, LocalDate start, LocalDate end);

    List<Expense> findByTenantIdAndVendorIgnoreCase(String tenantId, String vendor);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.tenantId = :tenantId")
    BigDecimal sumByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.tenantId = :tenantId " +
           "AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumByTenantIdAndDateRange(@Param("tenantId") String tenantId,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.tenantId = :tenantId " +
           "AND e.category = :category AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumByCategoryAndDateRange(@Param("tenantId") String tenantId,
                                         @Param("category") Expense.ExpenseCategory category,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.tenantId = :tenantId AND e.expenseDate BETWEEN :start AND :end " +
           "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumGroupedByCategoryAndDateRange(@Param("tenantId") String tenantId,
                                                     @Param("start") LocalDate start,
                                                     @Param("end") LocalDate end);

    long countByTenantId(String tenantId);

    // Duplicate detection: same vendor + same amount + same date
    @Query("SELECT e FROM Expense e WHERE e.tenantId = :tenantId " +
           "AND e.vendor = :vendor AND e.amount = :amount AND e.expenseDate = :date AND e.id <> :excludeId")
    List<Expense> findPotentialDuplicates(@Param("tenantId") String tenantId,
                                          @Param("vendor") String vendor,
                                          @Param("amount") BigDecimal amount,
                                          @Param("date") LocalDate date,
                                          @Param("excludeId") Long excludeId);
}
