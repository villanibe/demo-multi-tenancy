package com.demo.multitenancy.billing.gateway;

import com.demo.multitenancy.billing.PaymentProperties;
import com.demo.multitenancy.subscription.domain.BillingInterval;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StripePaymentGateway implements PaymentGateway {
  private final PaymentProperties paymentProperties;

  public StripePaymentGateway(PaymentProperties paymentProperties) {
    this.paymentProperties = paymentProperties;
  }

  @Override
  public String provider() {
    return "stripe";
  }

  @Override
  public PaymentGatewayCheckoutSession createCheckoutSession(
      String tenantId,
      String providerPriceId,
      String planCode,
      BillingInterval interval) {
    String apiKey = paymentProperties.getStripe().getApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      // Intentionally fail fast: in real integration you would call Stripe API.
      throw new IllegalStateException("Stripe apiKey not configured (app.payments.stripe.apiKey)");
    }

    // Placeholder URL for the blueprint.
    String sessionId = "cs_test_" + UUID.randomUUID();
    String url = "https://checkout.stripe.com/pay/placeholder?tenant=" + tenantId + "&plan=" + planCode + "&price=" + providerPriceId + "&session=" + sessionId;
    return new PaymentGatewayCheckoutSession(sessionId, url);
  }
}

