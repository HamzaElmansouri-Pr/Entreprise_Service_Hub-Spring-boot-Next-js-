package nova.enterprise_service_hub.controller;

import com.stripe.exception.StripeException;
import nova.enterprise_service_hub.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment Controller — Phase 7.2
 * <p>
 * Creates Stripe Checkout sessions for invoice payments.
 */
@RestController
@RequestMapping("/v1/payments")
@PreAuthorize("hasRole('ADMIN')")
public class PaymentController {

    private final StripeService stripeService;

    public PaymentController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckout(
            @RequestBody Map<String, Object> request) throws StripeException {

        String invoiceRef = (String) request.get("invoiceRef");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String currency = (String) request.getOrDefault("currency", "usd");
        String tenantId = (String) request.get("tenantId");

        String url = stripeService.createCheckoutSession(invoiceRef, amount, currency, tenantId);
        return ResponseEntity.ok(Map.of("checkoutUrl", url));
    }
}
