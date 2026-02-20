package com.demo.multitenancy.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payments")
public class PaymentProperties {
  private PaymentProvider provider = PaymentProvider.STRIPE;
  private final Stripe stripe = new Stripe();

  public PaymentProvider getProvider() {
    return provider;
  }

  public void setProvider(PaymentProvider provider) {
    this.provider = provider;
  }

  public Stripe getStripe() {
    return stripe;
  }

  public static class Stripe {
    private String apiKey;

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }
  }
}
