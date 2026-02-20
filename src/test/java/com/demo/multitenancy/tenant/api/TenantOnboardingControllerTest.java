package com.demo.multitenancy.tenant.api;

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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TenantOnboardingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void createTenant_withoutJwt_thenUnauthorized() throws Exception {
    mockMvc.perform(post("/api/tenants")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantId\":\"t-acme\",\"displayName\":\"Acme\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createTenant_withJwt_thenOkAndMineListsIt() throws Exception {
    mockMvc.perform(post("/api/tenants")
            .with(jwt().jwt(jwt -> jwt.subject("sub-1").claim("email", "a@b.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantId\":\"t-acme\",\"displayName\":\"Acme\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantId").value("t-acme"));

    mockMvc.perform(get("/api/tenants/mine")
            .with(jwt().jwt(jwt -> jwt.subject("sub-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tenantId").value("t-acme"))
        .andExpect(jsonPath("$[0].role").value("OWNER"));
  }
}
