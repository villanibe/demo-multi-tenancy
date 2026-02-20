package com.demo.multitenancy.user.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class OrganizationAccessAuthorizationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void rolesAdmin_whenMember_thenForbidden_whenOwner_thenCanGrantPermissionToMemberRole() throws Exception {
    // Owner creates tenant
    mockMvc.perform(post("/api/tenants")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("email", "owner@acme.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantId\":\"t-authz\",\"displayName\":\"AuthzCo\"}"))
        .andExpect(status().isOk());

    // Owner invites member
    String inviteJson = mockMvc.perform(post("/api/tenants/t-authz/invites")
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

    // Member forbidden to read roles
    mockMvc.perform(get("/api/organization-access/roles")
            .header("X-Tenant-Id", "t-authz")
            .with(jwt().jwt(jwt -> jwt.subject("member-sub").claim("tenant_id", "t-authz"))))
        .andExpect(status().isForbidden());

    // Owner can read roles
    mockMvc.perform(get("/api/organization-access/roles")
            .header("X-Tenant-Id", "t-authz")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("tenant_id", "t-authz"))))
        .andExpect(status().isOk());

    // Owner grants billing.write to MEMBER
    mockMvc.perform(put("/api/organization-access/roles/MEMBER/permissions/billing.write")
            .header("X-Tenant-Id", "t-authz")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("tenant_id", "t-authz"))))
        .andExpect(status().isOk());
  }
}
