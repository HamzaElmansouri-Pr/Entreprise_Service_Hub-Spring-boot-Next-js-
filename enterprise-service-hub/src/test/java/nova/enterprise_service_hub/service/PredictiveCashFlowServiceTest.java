package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.AiBusinessIntelligenceDTO.CashFlowForecast;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PredictiveCashFlowService — validates 90-day forecast generation.
 */
@ExtendWith(MockitoExtension.class)
class PredictiveCashFlowServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private AiReportStore reportStore;

    @InjectMocks private PredictiveCashFlowService cashFlowService;

    private static final String TENANT = "test-tenant";
    private static final String REPORT_ID = "forecast-001";

    @BeforeEach
    void setUp() {
        doNothing().when(reportStore).updateStatus(anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    // ── Helper factories ─────────────────────────────────────────────────

    private Invoice createPaidInvoice(Long id, BigDecimal amount, LocalDate createdDate) {
        Invoice inv = new Invoice();
        inv.setId(id);
        inv.setTenantId(TENANT);
        inv.setAmount(amount);
        inv.setStatus(Invoice.InvoiceStatus.PAID);
        inv.setReferenceNumber("INV-" + id);
        inv.setDueDate(createdDate);
        inv.setCreatedAt(createdDate.atStartOfDay().toInstant(ZoneOffset.UTC));
        return inv;
    }

    private Expense createExpense(Long id, BigDecimal amount, LocalDate date) {
        Expense exp = new Expense();
        exp.setId(id);
        exp.setTenantId(TENANT);
        exp.setAmount(amount);
        exp.setCategory(Expense.ExpenseCategory.SOFTWARE_LICENSE);
        exp.setVendor("Test Vendor");
        exp.setExpenseDate(date);
        exp.setDescription("Test expense");
        return exp;
    }

    // ── Forecast Structure ───────────────────────────────────────────────

    @Nested
    @DisplayName("Forecast Structure")
    class ForecastStructure {

        @Test
        @DisplayName("should return a valid 3-month forecast")
        void valid3MonthForecast() {
            // Create 6 months of revenue data
            List<Invoice> invoices = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusMonths(i).withDayOfMonth(15);
                invoices.add(createPaidInvoice((long) (6 - i), BigDecimal.valueOf(10000 + i * 500), date));
            }

            List<Expense> expenses = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusMonths(i).withDayOfMonth(10);
                expenses.add(createExpense((long) (6 - i), BigDecimal.valueOf(3000 + i * 100), date));
            }

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(expenses);
            when(invoiceRepository.sumPaidByTenantId(TENANT)).thenReturn(BigDecimal.valueOf(63000));
            when(expenseRepository.sumByTenantId(TENANT)).thenReturn(BigDecimal.valueOf(19500));

            CashFlowForecast forecast = cashFlowService.generateForecast(TENANT, REPORT_ID);

            assertThat(forecast.tenantId()).isEqualTo(TENANT);
            assertThat(forecast.monthlyBreakdown()).hasSize(3);
            assertThat(forecast.generatedAt()).isNotNull();
            assertThat(forecast.confidence()).isIn("HIGH", "MEDIUM", "LOW");
            assertThat(forecast.riskFactors()).isNotNull();
        }

        @Test
        @DisplayName("monthly breakdowns should have revenue, expenses, and net")
        void monthlyBreakdownFields() {
            List<Invoice> invoices = List.of(
                    createPaidInvoice(1L, BigDecimal.valueOf(15000), LocalDate.now().minusMonths(1))
            );
            List<Expense> expenses = List.of(
                    createExpense(1L, BigDecimal.valueOf(5000), LocalDate.now().minusMonths(1))
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(expenses);
            when(invoiceRepository.sumPaidByTenantId(TENANT)).thenReturn(BigDecimal.valueOf(15000));
            when(expenseRepository.sumByTenantId(TENANT)).thenReturn(BigDecimal.valueOf(5000));

            CashFlowForecast forecast = cashFlowService.generateForecast(TENANT, REPORT_ID);

            forecast.monthlyBreakdown().forEach(m -> {
                assertThat(m.month()).isNotBlank();
                assertThat(m.expectedRevenue()).isNotNull();
                assertThat(m.expectedExpenses()).isNotNull();
                // Net = Revenue - Expenses
                assertThat(m.netCashFlow().doubleValue())
                        .isCloseTo(
                                m.expectedRevenue().subtract(m.expectedExpenses()).doubleValue(),
                                org.assertj.core.data.Offset.offset(0.01));
            });
        }
    }

    // ── Current Balance ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Current Balance Calculation")
    class CurrentBalance {

        @Test
        @DisplayName("balance = total paid invoices - total expenses")
        void balanceCalculation() {
            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of());
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());
            when(invoiceRepository.sumPaidByTenantId(TENANT)).thenReturn(BigDecimal.valueOf(50000));
            when(expenseRepository.sumByTenantId(TENANT)).thenReturn(BigDecimal.valueOf(20000));

            CashFlowForecast forecast = cashFlowService.generateForecast(TENANT, REPORT_ID);

            assertThat(forecast.currentBalance()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        }
    }

    // ── Risk Factor Detection ────────────────────────────────────────────

    @Nested
    @DisplayName("Risk Factor Detection")
    class RiskFactors {

        @Test
        @DisplayName("should identify overdue invoices as a risk")
        void overdueInvoiceRisk() {
            Invoice overdue = new Invoice();
            overdue.setId(1L);
            overdue.setTenantId(TENANT);
            overdue.setAmount(BigDecimal.valueOf(15000));
            overdue.setStatus(Invoice.InvoiceStatus.OVERDUE);
            overdue.setDueDate(LocalDate.now().minusDays(60));
            overdue.setCreatedAt(Instant.now());

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of(overdue));
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());
            when(invoiceRepository.sumPaidByTenantId(TENANT)).thenReturn(BigDecimal.ZERO);
            when(expenseRepository.sumByTenantId(TENANT)).thenReturn(BigDecimal.ZERO);

            CashFlowForecast forecast = cashFlowService.generateForecast(TENANT, REPORT_ID);

            assertThat(forecast.riskFactors())
                    .anyMatch(r -> r.contains("overdue"));
        }
    }

    // ── Edge Cases ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty data gracefully")
        void emptyData() {
            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of());
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());
            when(invoiceRepository.sumPaidByTenantId(TENANT)).thenReturn(BigDecimal.ZERO);
            when(expenseRepository.sumByTenantId(TENANT)).thenReturn(BigDecimal.ZERO);

            CashFlowForecast forecast = cashFlowService.generateForecast(TENANT, REPORT_ID);

            assertThat(forecast.monthlyBreakdown()).hasSize(3);
            assertThat(forecast.predictedRevenue90d()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(forecast.predictedExpenses90d()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(forecast.confidence()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("should assign LOW confidence with sparse data")
        void lowConfidenceWithSparseData() {
            // Only 1 month of data
            List<Invoice> invoices = List.of(
                    createPaidInvoice(1L, BigDecimal.valueOf(10000), LocalDate.now().minusDays(5))
            );

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());
            when(invoiceRepository.sumPaidByTenantId(TENANT)).thenReturn(BigDecimal.valueOf(10000));
            when(expenseRepository.sumByTenantId(TENANT)).thenReturn(BigDecimal.ZERO);

            CashFlowForecast forecast = cashFlowService.generateForecast(TENANT, REPORT_ID);

            assertThat(forecast.confidence()).isIn("LOW", "MEDIUM");
        }

        @Test
        @DisplayName("predicted net = predicted revenue - predicted expenses")
        void netCashFlowConsistency() {
            List<Invoice> invoices = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                invoices.add(createPaidInvoice((long) (i + 1), BigDecimal.valueOf(10000),
                        LocalDate.now().minusMonths(i).withDayOfMonth(15)));
            }

            when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(invoices);
            when(expenseRepository.findByTenantIdOrderByExpenseDateDesc(TENANT)).thenReturn(List.of());
            when(invoiceRepository.sumPaidByTenantId(TENANT)).thenReturn(BigDecimal.valueOf(60000));
            when(expenseRepository.sumByTenantId(TENANT)).thenReturn(BigDecimal.ZERO);

            CashFlowForecast forecast = cashFlowService.generateForecast(TENANT, REPORT_ID);

            BigDecimal expectedNet = forecast.predictedRevenue90d()
                    .subtract(forecast.predictedExpenses90d());
            assertThat(forecast.predictedNetCashFlow()).isEqualByComparingTo(expectedNet);
        }
    }
}
