package com.demo.multitenancy.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payments")
public class PaymentProperties {
  private PaymentProvider provider = PaymentProvider.STRIPE;
  private final Stripe stripe = new Stripe();
  private final Webhooks webhooks = new Webhooks();

  public PaymentProvider getProvider() {
    return provider;
  }

  public void setProvider(PaymentProvider provider) {
    this.provider = provider;
  }

  public Stripe getStripe() {
    return stripe;
  }

  public Webhooks getWebhooks() {
    return webhooks;
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

  public static class Webhooks {
    private String secret;

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }
  }
}
