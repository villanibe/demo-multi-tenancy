package com.demo.multitenancy.billing.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.multitenancy.security.authorization.PermissionCodes;
import com.demo.multitenancy.tenant.TenantContextExecutor;
import com.demo.multitenancy.user.domain.Permission;
import com.demo.multitenancy.user.domain.PermissionRepository;
import com.demo.multitenancy.user.domain.Role;
import com.demo.multitenancy.user.domain.RoleRepository;

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
        "app.payments.stripe.apiKey=test-key"
    })
@AutoConfigureMockMvc
class BillingAuthorizationTest {

  @Autowired
  private MockMvc mockMvc;

    @Autowired
    private TenantContextExecutor tenantContextExecutor;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

  @Test
    void checkout_whenMember_thenForbidden_thenAfterGrantPermission_thenOk() throws Exception {
    // Owner creates tenant
    mockMvc.perform(post("/api/tenants")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("email", "owner@acme.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantId\":\"t-bill\",\"displayName\":\"BillCo\"}"))
        .andExpect(status().isOk());

    // Owner invites member
    String inviteJson = mockMvc.perform(post("/api/tenants/t-bill/invites")
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

    // Member forbidden
    mockMvc.perform(post("/api/billing/checkout")
            .header("X-Tenant-Id", "t-bill")
            .with(jwt().jwt(jwt -> jwt.subject("member-sub").claim("tenant_id", "t-bill")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planCode\":\"basic\"}"))
        .andExpect(status().isForbidden());

    // Flip tenant-scoped permissions in DB: grant billing.write to MEMBER
    tenantContextExecutor.runWithTenant("t-bill", () -> {
      Permission billingWrite = permissionRepository
          .findByTenantIdAndCode("t-bill", PermissionCodes.BILLING_WRITE)
          .orElseGet(() -> permissionRepository.save(new Permission(PermissionCodes.BILLING_WRITE)));

      Role memberRole = roleRepository
          .findWithPermissionsByTenantIdAndName("t-bill", "MEMBER")
          .orElseThrow();

      memberRole.getPermissions().add(billingWrite);
      roleRepository.save(memberRole);
      return null;
    });

    // Member now allowed (proves authorization consults persisted roles/permissions)
    mockMvc.perform(post("/api/billing/checkout")
            .header("X-Tenant-Id", "t-bill")
            .with(jwt().jwt(jwt -> jwt.subject("member-sub").claim("tenant_id", "t-bill")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planCode\":\"basic\"}"))
        .andExpect(status().isOk());
  }
}
