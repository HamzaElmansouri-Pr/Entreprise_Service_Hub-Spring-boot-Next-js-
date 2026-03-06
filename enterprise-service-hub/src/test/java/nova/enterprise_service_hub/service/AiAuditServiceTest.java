package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.AiBusinessIntelligenceDTO.AuditAnomaly;
import nova.enterprise_service_hub.dto.AiBusinessIntelligenceDTO.AuditReport;
import nova.enterprise_service_hub.event.AiReportStore;
import nova.enterprise_service_hub.model.Expense;
import nova.enterprise_service_hub.model.Invoice;
import nova.enterprise_service_hub.repository.ExpenseRepository;
import nova.enterprise_service_hub.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AiAuditService — validates all 6 anomaly detection algorithms.
 */
@ExtendWith(MockitoExtension.class)
class AiAuditServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private AiReportStore reportStore;

    @InjectMocks private AiAuditService auditService;

    private static final String TENANT = "test-tenant";
    private static final String REPORT_ID = "test-report-001";

    @BeforeEach
    void setUp() {
        // reportStore.updateStatus is void — just stub to do nothing
        doNothing().when(reportStore).updateStatus(anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    // ── Helper factories ─────────────────────────────────────────────────

    private Invoice createInvoice(Long id, BigDecimal amount, Invoice.InvoiceStatus status,
                                   String refNumber, LocalDate dueDate) {
        Invoice inv = new Invoice();
        inv.setId(id);
        inv.setTenantId(TENANT);
        inv.setAmount(amount);
        inv.setStatus(status);
        inv.setReferenceNumber(refNumber);
        inv.setDueDate(dueDate);
        inv.setCreatedAt(Instant.now());
        return inv;
    }

    private Expense createExpense(Long id, BigDecimal amount, Expense.ExpenseCategory category,
                                   String vendor, LocalDate expenseDate) {
        Expense exp = new Expense();
        exp.setId(id);
        exp.setTenantId(TENANT);
        exp.setAmount(amount);
        exp.setCategory(category);
        exp.setVendor(vendor);
        exp.setExpenseDate(expenseDate);
        exp.setDescription("Test expense");
        return exp;
    }

    // ── Overdue Invoice Detection ────────────────────────────────────────

    @Nested
    @DisplayName("Overdue Invoice Detection")
    class OverdueDetection {

        @Test
        @DisplayName("should flag invoices >30 days past due")
        void flagsOverdueInvoices() {
            List<Invoice> invoices = List.of(
                    createInvoice(1L, BigDecimal.valueOf(5000), Invoice.InvoiceStatus.PENDING,
                            "INV-001", LocalDate.now().minusDays(45)),
                    createInvoice(2L, BigDecimal.valueOf(3000), Invoice.InvoiceStatus.PAID,
                            "INV-002", LocalDate.now().minusDays(45))
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.anomalies()).isNotEmpty();
            assertThat(report.anomalies())
                    .anyMatch(a -> a.type().equals("OVERDUE_INVOICE") && a.entityId() == 1L);
            // Paid invoice should not be flagged
            assertThat(report.anomalies())
                    .noneMatch(a -> a.type().equals("OVERDUE_INVOICE") && a.entityId() == 2L);
        }

        @Test
        @DisplayName("should assign CRITICAL severity for >90 days overdue")
        void criticalSeverityFor90Days() {
            List<Invoice> invoices = List.of(
                    createInvoice(1L, BigDecimal.valueOf(10000), Invoice.InvoiceStatus.OVERDUE,
                            "INV-001", LocalDate.now().minusDays(120))
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.anomalies())
                    .anyMatch(a -> a.severity().equals("CRITICAL") && a.entityId() == 1L);
        }

        @Test
        @DisplayName("should not flag invoices within 30 days")
        void noFlagForRecentInvoices() {
            List<Invoice> invoices = List.of(
                    createInvoice(1L, BigDecimal.valueOf(5000), Invoice.InvoiceStatus.PENDING,
                            "INV-001", LocalDate.now().minusDays(15))
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.anomalies())
                    .noneMatch(a -> a.type().equals("OVERDUE_INVOICE"));
        }
    }

    // ── Duplicate Invoice Detection ──────────────────────────────────────

    @Nested
    @DisplayName("Duplicate Invoice Detection")
    class DuplicateInvoiceDetection {

        @Test
        @DisplayName("should detect invoices with same reference number and amount")
        void detectsDuplicateInvoices() {
            List<Invoice> invoices = List.of(
                    createInvoice(1L, BigDecimal.valueOf(5000), Invoice.InvoiceStatus.PENDING,
                            "INV-001", LocalDate.now().plusDays(30)),
                    createInvoice(2L, BigDecimal.valueOf(5000), Invoice.InvoiceStatus.PENDING,
                            "INV-001", LocalDate.now().plusDays(30))
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.duplicates()).isNotEmpty();
        }

        @Test
        @DisplayName("should not flag unique invoices as duplicates")
        void noDuplicatesForUnique() {
            List<Invoice> invoices = List.of(
                    createInvoice(1L, BigDecimal.valueOf(5000), Invoice.InvoiceStatus.PENDING,
                            "INV-001", LocalDate.now().plusDays(30)),
                    createInvoice(2L, BigDecimal.valueOf(3000), Invoice.InvoiceStatus.PENDING,
                            "INV-002", LocalDate.now().plusDays(30))
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.duplicates()).isEmpty();
        }
    }

    // ── Duplicate Expense Detection ──────────────────────────────────────

    @Nested
    @DisplayName("Duplicate Expense Detection")
    class DuplicateExpenseDetection {

        @Test
        @DisplayName("should detect potential duplicate expenses via repository")
        void detectsDuplicateExpenses() {
            LocalDate today = LocalDate.now();
            Expense exp1 = createExpense(1L, BigDecimal.valueOf(500), Expense.ExpenseCategory.SOFTWARE_LICENSE,
                    "Vendor A", today);
            Expense exp2 = createExpense(2L, BigDecimal.valueOf(500), Expense.ExpenseCategory.SOFTWARE_LICENSE,
                    "Vendor A", today);

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of());
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of(exp1, exp2));
            when(expenseRepository.findPotentialDuplicates(eq(TENANT), anyString(), any(BigDecimal.class),
                    any(LocalDate.class), anyLong()))
                    .thenReturn(List.of(exp2));

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.duplicates()).isNotEmpty();
        }
    }

    // ── Missing Metadata Detection ───────────────────────────────────────

    @Nested
    @DisplayName("Missing Metadata Detection")
    class MissingMetadataDetection {

        @Test
        @DisplayName("should flag invoices without reference numbers")
        void flagsMissingRefNumber() {
            List<Invoice> invoices = List.of(
                    createInvoice(1L, BigDecimal.valueOf(5000), Invoice.InvoiceStatus.PENDING,
                            null, LocalDate.now().plusDays(30)),
                    createInvoice(2L, BigDecimal.valueOf(5000), Invoice.InvoiceStatus.PENDING,
                            "", LocalDate.now().plusDays(30))
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            long missingRefCount = report.anomalies().stream()
                    .filter(a -> a.type().equals("MISSING_REFERENCE"))
                    .count();
            assertThat(missingRefCount).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("should flag expenses without vendors")
        void flagsMissingVendor() {
            Expense exp = createExpense(1L, BigDecimal.valueOf(100), Expense.ExpenseCategory.OFFICE,
                    null, LocalDate.now());

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of());
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of(exp));

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.anomalies())
                    .anyMatch(a -> a.type().equals("MISSING_VENDOR") && a.entityId() == 1L);
        }
    }

    // ── Amount Outlier Detection ─────────────────────────────────────────

    @Nested
    @DisplayName("Amount Outlier Detection (Z-Score)")
    class AmountOutlierDetection {

        @Test
        @DisplayName("should detect outlier amounts using Z-score analysis")
        void detectsOutlierAmounts() {
            // Create 10 normal invoices + 1 extreme outlier
            List<Invoice> invoices = new ArrayList<>();
            for (long i = 1; i <= 10; i++) {
                invoices.add(createInvoice(i, BigDecimal.valueOf(1000 + i * 10),
                        Invoice.InvoiceStatus.PAID, "INV-" + i, LocalDate.now().plusDays(30)));
            }
            // Outlier: 100x normal
            invoices.add(createInvoice(11L, BigDecimal.valueOf(100_000),
                    Invoice.InvoiceStatus.PAID, "INV-11", LocalDate.now().plusDays(30)));

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.anomalies())
                    .anyMatch(a -> a.type().equals("AMOUNT_OUTLIER") && a.entityId() == 11L);
        }

        @Test
        @DisplayName("should not flag when too few records (<6)")
        void noOutliersWithFewRecords() {
            List<Invoice> invoices = List.of(
                    createInvoice(1L, BigDecimal.valueOf(1000), Invoice.InvoiceStatus.PAID,
                            "INV-1", LocalDate.now().plusDays(30)),
                    createInvoice(2L, BigDecimal.valueOf(100_000), Invoice.InvoiceStatus.PAID,
                            "INV-2", LocalDate.now().plusDays(30))
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.anomalies())
                    .noneMatch(a -> a.type().equals("AMOUNT_OUTLIER"));
        }
    }

    // ── Report Structure ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Report Structure & Metadata")
    class ReportStructure {

        @Test
        @DisplayName("should return valid report with correct counts")
        void validReportStructure() {
            List<Invoice> invoices = List.of(
                    createInvoice(1L, BigDecimal.valueOf(5000), Invoice.InvoiceStatus.PAID,
                            "INV-001", LocalDate.now())
            );
            List<Expense> expenses = List.of(
                    createExpense(1L, BigDecimal.valueOf(100), Expense.ExpenseCategory.OFFICE,
                            "OfficeMax", LocalDate.now())
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(expenses);

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.reportId()).isEqualTo(REPORT_ID);
            assertThat(report.tenantId()).isEqualTo(TENANT);
            assertThat(report.totalInvoicesScanned()).isEqualTo(1);
            assertThat(report.totalExpensesScanned()).isEqualTo(1);
            assertThat(report.generatedAt()).isNotNull();
            assertThat(report.summary()).isNotBlank();
            assertThat(report.status()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("should handle empty data gracefully")
        void emptyDataGraceful() {
            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of());
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());

            AuditReport report = auditService.runAudit(TENANT, REPORT_ID);

            assertThat(report.totalInvoicesScanned()).isZero();
            assertThat(report.totalExpensesScanned()).isZero();
            assertThat(report.anomalies()).isEmpty();
            assertThat(report.duplicates()).isEmpty();
            assertThat(report.status()).isEqualTo("COMPLETED");
        }
    }
}
