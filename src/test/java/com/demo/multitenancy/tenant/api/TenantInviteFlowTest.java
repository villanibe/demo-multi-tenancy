package com.demo.multitenancy.tenant.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TenantInviteFlowTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void inviteAndAccept_flowCreatesMembership() throws Exception {
    // Owner creates tenant
    mockMvc.perform(post("/api/tenants")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("email", "owner@acme.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantId\":\"t-invite\",\"displayName\":\"InviteCo\"}"))
        .andExpect(status().isOk());

    // Owner invites
    String inviteToken = mockMvc.perform(post("/api/tenants/t-invite/invites")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"new@acme.com\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantId").value("t-invite"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    // Extract token cheaply (keep dependencies minimal)
    String token = inviteToken.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    // Invitee accepts
    mockMvc.perform(post("/api/tenants/invites/accept")
            .with(jwt().jwt(jwt -> jwt.subject("invitee-sub").claim("email", "new@acme.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantId").value("t-invite"))
        .andExpect(jsonPath("$.role").value("MEMBER"));
  }
}
