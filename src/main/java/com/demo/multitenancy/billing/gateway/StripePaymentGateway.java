package com.demo.multitenancy.billing.gateway;

import com.demo.multitenancy.billing.PaymentProperties;

import org.springframework.stereotype.Component;

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
  public String createCheckoutUrl(String tenantId, String providerPriceId, String planCode) {
    String apiKey = paymentProperties.getStripe().getApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      // Intentionally fail fast: in real integration you would call Stripe API.
      throw new IllegalStateException("Stripe apiKey not configured (app.payments.stripe.apiKey)");
    }

    // Placeholder URL for the blueprint.
    return "https://checkout.stripe.com/pay/placeholder?tenant=" + tenantId + "&plan=" + planCode + "&price=" + providerPriceId;
  }
}
