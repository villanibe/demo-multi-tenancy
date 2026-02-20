package com.demo.multitenancy.user.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import com.demo.multitenancy.tenant.TenantIdResolver;
import com.demo.multitenancy.user.domain.UserAccount;
import com.demo.multitenancy.user.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(controllers = UserController.class)
class UserControllerSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @MockBean
  private TenantIdResolver tenantIdResolver;

  @Test
  void getByEmail_withoutJwt_thenUnauthorized() throws Exception {
    mockMvc.perform(get("/api/users").param("email", "a@b.com"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getByEmail_withJwt_thenOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(new UserAccount("a@b.com", "Alice")));

    mockMvc.perform(get("/api/users")
            .param("email", "a@b.com")
            .header("X-Tenant-Id", "tenant-a")
            .with(jwt().jwt(jwt -> jwt.claim("tenant_id", "tenant-a"))))
        .andExpect(status().isOk());
  }
}
