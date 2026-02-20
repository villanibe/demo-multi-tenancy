package com.demo.multitenancy.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ActuatorSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void health_isPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void info_isPublic() throws Exception {
    mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
  }

  @Test
  void metrics_isProtectedWithoutJwt() throws Exception {
    mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
  }
}
