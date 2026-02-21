package com.demo.multitenancy.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.multitenancy.security.auth.service.IdentityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class LocalAuthFlowTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private IdentityService identityService;

  @Test
  void login_thenRefresh_thenCallProtectedEndpoint() throws Exception {
    identityService.createIdentity("usr_test", "user@example.com", "ChangeMe!123");

    MvcResult login = mockMvc.perform(post("/api/public/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"user@example.com\",\"password\":\"ChangeMe!123\"}"))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode loginJson = objectMapper.readTree(login.getResponse().getContentAsString());
    String accessToken = loginJson.get("accessToken").asText();
    String refreshToken = loginJson.get("refreshToken").asText();

    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();

    MvcResult refresh = mockMvc.perform(post("/api/public/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode refreshJson = objectMapper.readTree(refresh.getResponse().getContentAsString());
    String accessToken2 = refreshJson.get("accessToken").asText();
    String refreshToken2 = refreshJson.get("refreshToken").asText();

    assertThat(accessToken2).isNotBlank();
    assertThat(refreshToken2).isNotBlank();
    assertThat(refreshToken2).isNotEqualTo(refreshToken);

    mockMvc.perform(get("/api/tenant/current")
            .header("Authorization", "Bearer " + accessToken2)
            .header("X-Tenant-Id", "tenant-a"))
        .andExpect(status().isOk());
  }
}
