package com.demo.multitenancy.billing.webhook;

/**
 * Legacy placeholder.
 *
 * Webhook routing is now handled by {@link PaymentWebhookRegistry}, which builds provider-keyed
 * hash maps for both {@link PaymentWebhookHandler} and {@link WebhookVerifier}.
 */
public final class PaymentWebhookConfiguration {
  private PaymentWebhookConfiguration() {
  }
}
