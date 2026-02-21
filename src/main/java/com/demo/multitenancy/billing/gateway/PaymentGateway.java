package com.demo.multitenancy.billing.gateway;

import com.demo.multitenancy.subscription.domain.BillingInterval;

public interface PaymentGateway {
  String provider();

  PaymentGatewayCheckoutSession createCheckoutSession(
      String tenantId,
      String providerPriceId,
      String planCode,
      BillingInterval interval);
}

