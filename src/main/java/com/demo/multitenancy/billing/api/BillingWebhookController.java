package com.demo.multitenancy.billing.api;

import java.util.Locale;
import java.util.Map;

import com.demo.multitenancy.billing.webhook.PaymentWebhookHandler;
import com.demo.multitenancy.billing.webhook.PaymentWebhookRegistry;
import com.demo.multitenancy.billing.webhook.WebhookVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/billing/webhooks")
public class BillingWebhookController {

  private final ObjectMapper objectMapper;
  private final PaymentWebhookRegistry registry;

  public BillingWebhookController(
      ObjectMapper objectMapper,
      PaymentWebhookRegistry registry) {
    this.objectMapper = objectMapper;
    this.registry = registry;
  }

  @PostMapping("/{provider}")
  public ResponseEntity<Map<String, Object>> handle(
      @PathVariable String provider,
      @RequestHeader HttpHeaders headers,
      @RequestBody String rawBody) {

    String providerKey = normalizeKey(provider);

    WebhookVerifier verifier = registry.getVerifier(providerKey);
    if (verifier == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown webhook verifier for provider: " + provider);
    }
    verifier.verify(headers, rawBody);

    PaymentWebhookHandler handler = registry.getHandler(providerKey);
    if (handler == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown webhook handler for provider: " + provider);
    }

    JsonNode payload = parseJson(rawBody);
    handler.handle(payload);
    return ResponseEntity.ok(Map.of("status", "ok"));
  }

  private String normalizeKey(String value) {
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown provider");
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private JsonNode parseJson(String rawBody) {
    try {
      return objectMapper.readTree(rawBody);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON body");
    }
  }

}
