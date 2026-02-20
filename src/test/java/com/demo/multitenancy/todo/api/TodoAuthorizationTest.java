package com.demo.multitenancy.todo.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TodoAuthorizationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void lists_withoutJwt_thenUnauthorized() throws Exception {
    mockMvc.perform(get("/api/todos/lists"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void lists_withJwtNoScopeAndNotMember_thenForbidden() throws Exception {
    mockMvc.perform(get("/api/todos/lists")
            .header("X-Tenant-Id", "tenant-x")
            .with(jwt().jwt(jwt -> jwt.subject("sub-x").claim("tenant_id", "tenant-x"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void lists_withJwtAndScope_thenOk() throws Exception {
    mockMvc.perform(get("/api/todos/lists")
            .header("X-Tenant-Id", "tenant-x")
            .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_todo.read"))
                .jwt(jwt -> jwt.subject("sub-x").claim("tenant_id", "tenant-x"))))
        .andExpect(status().isOk());
  }

  @Test
  void lists_withJwtNoScopeButOwnerMembership_thenOk() throws Exception {
    // Create tenant (control-plane) => grants OWNER membership to subject
    mockMvc.perform(post("/api/tenants")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("email", "owner@demo.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantId\":\"t-auth\",\"displayName\":\"AuthCo\"}"))
        .andExpect(status().isOk());

    // Access tenant-scoped endpoint without scope, relying on membership role
    mockMvc.perform(get("/api/todos/lists")
            .header("X-Tenant-Id", "t-auth")
            .with(jwt().jwt(jwt -> jwt.subject("owner-sub").claim("tenant_id", "t-auth"))))
        .andExpect(status().isOk());
  }
}
