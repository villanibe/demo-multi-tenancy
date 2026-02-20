package com.demo.multitenancy.billing.gateway;

public interface PaymentGateway {
  String provider();

  String createCheckoutUrl(String tenantId, String planCode);
}
