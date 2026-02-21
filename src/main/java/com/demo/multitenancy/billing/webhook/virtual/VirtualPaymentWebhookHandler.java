package com.demo.multitenancy.billing.webhook.virtual;

import com.demo.multitenancy.billing.service.VirtualWebhookProcessor;
import com.demo.multitenancy.billing.webhook.PaymentWebhookHandler;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class VirtualPaymentWebhookHandler implements PaymentWebhookHandler {

  private final VirtualWebhookProcessor virtualWebhookProcessor;

  public VirtualPaymentWebhookHandler(VirtualWebhookProcessor virtualWebhookProcessor) {
    this.virtualWebhookProcessor = virtualWebhookProcessor;
  }

  @Override
  public String provider() {
    return "virtual";
  }

  @Override
  public void handle(JsonNode payload) {
    VirtualWebhookRequest request = new VirtualWebhookRequest();
    request.setEventId(text(payload, "eventId"));
    request.setType(text(payload, "type"));
    request.setSessionId(text(payload, "sessionId"));

    if (isBlank(request.getEventId()) || isBlank(request.getType()) || isBlank(request.getSessionId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields: eventId/type/sessionId");
    }

    virtualWebhookProcessor.handle(request);
  }

  private static String text(JsonNode payload, String field) {
    if (payload == null || payload.get(field) == null || payload.get(field).isNull()) {
      return null;
    }
    return payload.get(field).asText(null);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
