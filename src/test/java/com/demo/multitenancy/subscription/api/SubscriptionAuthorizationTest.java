package com.demo.multitenancy.subscription.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.multitenancy.subscription.domain.Plan;
import com.demo.multitenancy.subscription.domain.PlanRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SubscriptionAuthorizationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PlanRepository planRepository;

  @Test
  void trial_whenMember_thenForbidden_butCurrentAllowed() throws Exception {
        planRepository.save(new Plan("basic-sub", "Basic", 7));

    // Owner creates tenant
    mockMvc.perform(post("/api/tenants")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("email", "owner@acme.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantId\":\"t-sub\",\"displayName\":\"SubCo\"}"))
        .andExpect(status().isOk());

    // Owner starts trial (creates subscription)
    mockMvc.perform(post("/api/subscription/trial")
            .header("X-Tenant-Id", "t-sub")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("tenant_id", "t-sub")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planCode\":\"basic-sub\"}"))
        .andExpect(status().isOk());

    // Owner invites member
    String inviteJson = mockMvc.perform(post("/api/tenants/t-sub/invites")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"member@acme.com\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String token = inviteJson.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    // Member accepts invite
    mockMvc.perform(post("/api/tenants/invites/accept")
            .with(jwt().jwt(jwt -> jwt.subject("member-sub").claim("email", "member@acme.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\"}"))
        .andExpect(status().isOk());

    // Member cannot start trial
    mockMvc.perform(post("/api/subscription/trial")
            .header("X-Tenant-Id", "t-sub")
            .with(jwt().jwt(jwt -> jwt.subject("member-sub").claim("tenant_id", "t-sub")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planCode\":\"basic-sub\"}"))
        .andExpect(status().isForbidden());

    // Member can read current
    mockMvc.perform(get("/api/subscription/current")
            .header("X-Tenant-Id", "t-sub")
            .with(jwt().jwt(jwt -> jwt.subject("member-sub").claim("tenant_id", "t-sub"))))
        .andExpect(status().isOk());
  }
}
