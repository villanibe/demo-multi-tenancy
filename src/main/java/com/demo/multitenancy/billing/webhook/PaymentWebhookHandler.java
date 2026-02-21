package com.demo.multitenancy.billing.webhook;

import com.fasterxml.jackson.databind.JsonNode;

public interface PaymentWebhookHandler {
  String provider();

  void handle(JsonNode payload);
}
