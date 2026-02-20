package com.demo.multitenancy.user.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.multitenancy.subscription.domain.Plan;
import com.demo.multitenancy.subscription.domain.PlanRepository;
import com.demo.multitenancy.subscription.planfeature.PlanFeatureCodes;

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
class UserAuthorizationTest {

  @Autowired
  private MockMvc mockMvc;

    @Autowired
    private PlanRepository planRepository;

  @Test
  void userRead_memberAllowed_userWriteMemberForbidden_ownerAllowed() throws Exception {
        Plan basic = new Plan("basic-users", "Basic Users", 7);
        basic.getPlanFeatureCodes().add(PlanFeatureCodes.USERS_MANAGE);
        planRepository.save(basic);

    // Owner creates tenant
    mockMvc.perform(post("/api/tenants")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("email", "owner@acme.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantId\":\"t-user\",\"displayName\":\"UserCo\"}"))
        .andExpect(status().isOk());

    // Owner invites member
    String inviteJson = mockMvc.perform(post("/api/tenants/t-user/invites")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"member@acme.com\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String token = inviteJson.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    // Member accepts
    mockMvc.perform(post("/api/tenants/invites/accept")
            .with(jwt().jwt(jwt -> jwt.subject("member-sub").claim("email", "member@acme.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\"}"))
        .andExpect(status().isOk());

    // Owner cannot create a user without subscription entitlement
    mockMvc.perform(post("/api/users")
            .header("X-Tenant-Id", "t-user")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("tenant_id", "t-user")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"a@b.com\",\"displayName\":\"Alice\"}"))
        .andExpect(status().isForbidden());

    // Owner starts trial (creates subscription and activates plan entitlements)
    mockMvc.perform(post("/api/subscription/trial")
            .header("X-Tenant-Id", "t-user")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("tenant_id", "t-user")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planCode\":\"basic-users\"}"))
        .andExpect(status().isOk());

    // Owner creates a user
    mockMvc.perform(post("/api/users")
            .header("X-Tenant-Id", "t-user")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("tenant_id", "t-user")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"a@b.com\",\"displayName\":\"Alice\"}"))
        .andExpect(status().isOk());

    // Member can read by email
    mockMvc.perform(get("/api/users")
            .param("email", "a@b.com")
            .header("X-Tenant-Id", "t-user")
            .with(jwt().jwt(jwt -> jwt.subject("member-sub").claim("tenant_id", "t-user"))))
        .andExpect(status().isOk());

    // Member cannot create users
    mockMvc.perform(post("/api/users")
            .header("X-Tenant-Id", "t-user")
            .with(jwt().jwt(jwt -> jwt.subject("member-sub").claim("tenant_id", "t-user")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"c@d.com\",\"displayName\":\"Carol\"}"))
        .andExpect(status().isForbidden());
  }
}
