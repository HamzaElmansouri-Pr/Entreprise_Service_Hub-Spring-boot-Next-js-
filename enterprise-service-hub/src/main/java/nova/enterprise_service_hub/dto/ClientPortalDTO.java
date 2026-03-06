package nova.enterprise_service_hub.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Client Portal dashboard view — aggregated data for a specific client.
 */
public record ClientPortalDTO(
        String clientName,
        String companyName,
        List<PortalProject> projects,
        List<PortalInvoice> invoices,
        BigDecimal totalPaid,
        BigDecimal totalOutstanding) {

    public record PortalProject(
            Long id,
            String name,
            String status,
            int progressPercent,
            String lastUpdateNote) {
    }

    public record PortalInvoice(
            Long id,
            String referenceNumber,
            BigDecimal amount,
            String status,
            String dueDate,
            String checkoutUrl) {
    }
}
