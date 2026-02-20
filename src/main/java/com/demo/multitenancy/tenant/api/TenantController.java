package com.demo.multitenancy.tenant.api;

import java.util.Map;

import com.demo.multitenancy.tenant.TenantContext;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

  @Operation(summary = "Get resolved tenant context")
  @GetMapping("/current")
  public ResponseEntity<Map<String, Object>> current(Authentication authentication) {
    return ResponseEntity.ok(Map.of(
        "tenantId", TenantContext.getTenantId().orElse(null),
        "authenticated", authentication != null,
        "principal", authentication != null ? authentication.getName() : null));
  }
}
