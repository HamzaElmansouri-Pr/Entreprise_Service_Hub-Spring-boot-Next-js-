package nova.enterprise_service_hub.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Stripe Payment Service — Phase 7.2
 * <p>
 * Creates Checkout Sessions and handles payment intents.
 */
@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.api-key:}")
    private String apiKey;

    @Value("${stripe.success-url:http://localhost:3000/payment/success}")
    private String successUrl;

    @Value("${stripe.cancel-url:http://localhost:3000/payment/cancel}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            Stripe.apiKey = apiKey;
            log.info("Stripe initialized with API key");
        } else {
            log.warn("Stripe API key not configured — payment features disabled");
        }
    }

    /**
     * Create a Stripe Checkout Session for an invoice payment.
     *
     * @param invoiceRef  Invoice reference number (used as line item name)
     * @param amount      Amount in major currency units (e.g., 99.99)
     * @param currency    ISO currency code (e.g., "usd")
     * @param tenantId    Tenant ID for metadata
     * @return Checkout session URL to redirect the customer to
     */
    public String createCheckoutSession(String invoiceRef, BigDecimal amount, String currency, String tenantId)
            throws StripeException {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Stripe is not configured. Set STRIPE_SECRET_KEY environment variable.");
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Invoice: " + invoiceRef)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("tenant_id", tenantId)
                .putMetadata("invoice_ref", invoiceRef)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}
