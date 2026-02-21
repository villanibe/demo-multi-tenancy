package com.demo.multitenancy.billing.webhook.stripe;

import com.demo.multitenancy.billing.webhook.WebhookVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StripeWebhookVerifier implements WebhookVerifier {

  @Override
  public String provider() {
    return "stripe";
  }

  @Override
  public void verify(HttpHeaders headers, String rawBody) {
    // Placeholder for future Stripe signature verification.
    // In a real implementation, verify the Stripe-Signature header against the configured signing secret
    // using the *raw request body*.
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Stripe webhook verification not implemented");
  }
}
