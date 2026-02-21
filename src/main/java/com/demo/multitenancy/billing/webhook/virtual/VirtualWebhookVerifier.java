package com.demo.multitenancy.billing.webhook.virtual;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.demo.multitenancy.billing.PaymentProperties;
import com.demo.multitenancy.billing.webhook.WebhookVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class VirtualWebhookVerifier implements WebhookVerifier {

  private static final String HEADER = "X-Webhook-Secret";

  private final PaymentProperties paymentProperties;

  public VirtualWebhookVerifier(PaymentProperties paymentProperties) {
    this.paymentProperties = paymentProperties;
  }

  @Override
  public String provider() {
    return "virtual";
  }

  @Override
  public void verify(HttpHeaders headers, String rawBody) {
    String expected = paymentProperties.getWebhooks().getSecret();
    if (expected == null || expected.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhooks secret not configured");
    }

    String provided = headers != null ? headers.getFirst(HEADER) : null;
    if (provided == null || provided.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing " + HEADER);
    }

    boolean matches = MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        provided.getBytes(StandardCharsets.UTF_8));

    if (!matches) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret");
    }
  }
}
