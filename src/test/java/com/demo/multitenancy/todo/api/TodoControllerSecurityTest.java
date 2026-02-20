package com.demo.multitenancy.todo.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.demo.multitenancy.tenant.TenantIdResolver;
import com.demo.multitenancy.todo.domain.TodoList;
import com.demo.multitenancy.todo.service.TodoService;
import com.demo.multitenancy.security.SecurityConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(controllers = TodoController.class)
@Import(SecurityConfiguration.class)
class TodoControllerSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TodoService todoService;

  @MockitoBean
  private TenantIdResolver tenantIdResolver;

  @Test
  void lists_withoutJwt_thenUnauthorized() throws Exception {
    mockMvc.perform(get("/api/todos/lists"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void lists_withJwtButMissingScope_thenForbidden() throws Exception {
    when(todoService.getLists()).thenReturn(List.of(new TodoList("Any", Instant.parse("2026-02-20T00:00:00Z"))));

    mockMvc.perform(get("/api/todos/lists")
            .header("X-Tenant-Id", "tenant-a")
            .with(jwt().jwt(jwt -> jwt.claim("tenant_id", "tenant-a"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void lists_withJwtAndScope_thenOk() throws Exception {
    when(todoService.getLists()).thenReturn(List.of(new TodoList("Any", Instant.parse("2026-02-20T00:00:00Z"))));

    mockMvc.perform(get("/api/todos/lists")
            .header("X-Tenant-Id", "tenant-a")
            .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_todo.read"))
                .jwt(jwt -> jwt.claim("tenant_id", "tenant-a"))))
        .andExpect(status().isOk());
  }
}
