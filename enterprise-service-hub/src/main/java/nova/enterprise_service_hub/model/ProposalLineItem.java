package nova.enterprise_service_hub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Line item within a Proposal — represents a single deliverable/charge.
 */
@Entity
@Table(name = "proposal_line_items")
@Getter
@Setter
@NoArgsConstructor
public class ProposalLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String description;

    @NotNull
    @Column(nullable = false)
    private int quantity = 1;

    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "display_order")
    private int displayOrder = 0;

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
