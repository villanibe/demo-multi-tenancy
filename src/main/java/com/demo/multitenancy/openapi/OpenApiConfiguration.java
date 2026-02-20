package com.demo.multitenancy.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

  @Bean
  public OpenAPI openAPI() {
    SecurityScheme bearer = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");

    return new OpenAPI()
        .info(new Info()
            .title("demo-multi-tenancy API")
            .version("0.0.1"))
        .components(new Components().addSecuritySchemes("bearer-jwt", bearer))
        .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
  }
}
