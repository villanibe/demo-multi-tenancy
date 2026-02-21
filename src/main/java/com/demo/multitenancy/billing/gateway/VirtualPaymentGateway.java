package com.demo.multitenancy.billing.gateway;

import java.util.UUID;

import com.demo.multitenancy.subscription.domain.BillingInterval;

import org.springframework.stereotype.Component;

@Component
public class VirtualPaymentGateway implements PaymentGateway {

  @Override
  public String provider() {
    return "virtual";
  }

  @Override
  public PaymentGatewayCheckoutSession createCheckoutSession(
      String tenantId,
      String providerPriceId,
      String planCode,
      BillingInterval interval) {
    String sessionId = "virt_" + UUID.randomUUID();
    String url = "https://virtual-payments.local/checkout/" + sessionId
        + "?tenant=" + tenantId
        + "&plan=" + planCode
        + "&price=" + providerPriceId
        + "&interval=" + interval.name().toLowerCase();
    return new PaymentGatewayCheckoutSession(sessionId, url);
  }
}
