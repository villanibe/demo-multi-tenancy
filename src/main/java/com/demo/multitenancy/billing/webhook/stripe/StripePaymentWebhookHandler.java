package com.demo.multitenancy.billing.webhook.stripe;

import com.demo.multitenancy.billing.webhook.PaymentWebhookHandler;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StripePaymentWebhookHandler implements PaymentWebhookHandler {

  @Override
  public String provider() {
    return "stripe";
  }

  @Override
  public void handle(JsonNode payload) {
    // Placeholder for Slice 5 blueprint; real implementation will verify signature, parse event types,
    // resolve checkout/subscription objects, and activate subscriptions.
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Stripe webhook handler not implemented");
  }
}
