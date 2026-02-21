package com.demo.multitenancy.billing.webhook;

import org.springframework.http.HttpHeaders;

public interface WebhookVerifier {
  String provider();

  void verify(HttpHeaders headers, String rawBody);
}
