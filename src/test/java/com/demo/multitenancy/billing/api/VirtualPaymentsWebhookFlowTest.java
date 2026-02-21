package com.demo.multitenancy.billing.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.payments.provider=VIRTUAL"
    })
@AutoConfigureMockMvc
class VirtualPaymentsWebhookFlowTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void checkout_thenWebhookCompletes_thenSubscriptionActive_andWebhookReplayIsIdempotent() throws Exception {
    // Create tenant (OWNER membership granted)
    mockMvc.perform(post("/api/tenants")
            .with(jwt().jwt(jwt -> jwt.subject("owner-pay").claim("email", "owner@acme.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantId\":\"t-pay\",\"displayName\":\"Pay\"}"))
        .andExpect(status().isOk());

    String checkoutJson = mockMvc.perform(post("/api/billing/checkout")
            .header("X-Tenant-Id", "t-pay")
            .with(jwt().jwt(jwt -> jwt.subject("owner-pay").claim("tenant_id", "t-pay")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planCode\":\"starter\",\"interval\":\"MONTHLY\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("virtual"))
        .andExpect(jsonPath("$.sessionId").isNotEmpty())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String sessionId = checkoutJson.replaceAll(".*\\\"sessionId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

    String webhookBody = "{\"eventId\":\"evt_1\",\"type\":\"checkout.session.completed\",\"sessionId\":\"" + sessionId + "\"}";

    mockMvc.perform(post("/api/billing/webhooks/virtual")
            .header("X-Webhook-Secret", "test-webhook-secret")
            .contentType(MediaType.APPLICATION_JSON)
            .content(webhookBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));

    // Replay the same event id: should be a no-op (idempotent)
    mockMvc.perform(post("/api/billing/webhooks/virtual")
            .header("X-Webhook-Secret", "test-webhook-secret")
            .contentType(MediaType.APPLICATION_JSON)
            .content(webhookBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));

    mockMvc.perform(get("/api/subscription/current")
            .header("X-Tenant-Id", "t-pay")
            .with(jwt().jwt(jwt -> jwt.subject("owner-pay").claim("tenant_id", "t-pay"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.planCode").value("starter"))
        .andExpect(jsonPath("$.currentPeriodEndsAt").isNotEmpty());
  }
}
