package com.demo.multitenancy.billing.gateway;

import java.util.List;

import com.demo.multitenancy.billing.PaymentProperties;

import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactory {
  private final PaymentProperties paymentProperties;
  private final List<PaymentGateway> gateways;

  public PaymentGatewayFactory(PaymentProperties paymentProperties, List<PaymentGateway> gateways) {
    this.paymentProperties = paymentProperties;
    this.gateways = gateways;
  }

  public PaymentGateway current() {
    String provider = paymentProperties.getProvider().name().toLowerCase();
    return gateways.stream()
        .filter(g -> g.provider().equalsIgnoreCase(provider))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No PaymentGateway registered for provider: " + provider));
  }
}
