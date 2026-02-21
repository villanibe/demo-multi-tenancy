package com.demo.multitenancy.billing.webhook;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookRegistry {

  private final Map<String, PaymentWebhookHandler> handlers;
  private final Map<String, WebhookVerifier> verifiers;

  public PaymentWebhookRegistry(List<PaymentWebhookHandler> handlers, List<WebhookVerifier> verifiers) {
    this.handlers = indexHandlers(handlers);
    this.verifiers = indexVerifiers(verifiers);
  }

  public PaymentWebhookHandler getHandler(String provider) {
    return handlers.get(normalizeKey(provider));
  }

  public WebhookVerifier getVerifier(String provider) {
    return verifiers.get(normalizeKey(provider));
  }

  private static Map<String, PaymentWebhookHandler> indexHandlers(List<PaymentWebhookHandler> handlers) {
    Map<String, PaymentWebhookHandler> byProvider = new HashMap<>();
    for (PaymentWebhookHandler handler : handlers) {
      String key = normalizeKey(handler.provider());
      PaymentWebhookHandler previous = byProvider.putIfAbsent(key, handler);
      if (previous != null) {
        throw new IllegalStateException("Duplicate PaymentWebhookHandler for provider: " + key);
      }
    }
    return Map.copyOf(byProvider);
  }

  private static Map<String, WebhookVerifier> indexVerifiers(List<WebhookVerifier> verifiers) {
    Map<String, WebhookVerifier> byProvider = new HashMap<>();
    for (WebhookVerifier verifier : verifiers) {
      String key = normalizeKey(verifier.provider());
      WebhookVerifier previous = byProvider.putIfAbsent(key, verifier);
      if (previous != null) {
        throw new IllegalStateException("Duplicate WebhookVerifier for provider: " + key);
      }
    }
    return Map.copyOf(byProvider);
  }

  private static String normalizeKey(String provider) {
    if (provider == null) {
      throw new IllegalStateException("Provider key is null");
    }
    String trimmed = provider.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalStateException("Provider key is blank");
    }
    return trimmed.toLowerCase(Locale.ROOT);
  }
}
