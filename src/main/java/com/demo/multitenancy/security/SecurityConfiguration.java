package com.demo.multitenancy.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.demo.multitenancy.tenant.TenantContextFilter;
import com.demo.multitenancy.tenant.TenantIdResolver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, TenantIdResolver tenantIdResolver) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

    http.addFilterAfter(new TenantContextFilter(tenantIdResolver), BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new JwtAuthoritiesConverter());
    return converter;
  }

  static class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      // Prefer "scope" / "scp" (OAuth2), fallback to "roles" for local tokens.
      Object scp = jwt.getClaims().get("scp");
      if (scp instanceof Collection<?> c) {
        return c.stream().filter(String.class::isInstance).map(String.class::cast)
        .map(s -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + s))
            .toList();
      }

      String scope = jwt.getClaimAsString("scope");
      if (scope != null && !scope.isBlank()) {
        return List.of(scope.split("\\s+"))
            .stream()
            .filter(s -> !s.isBlank())
        .map(s -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + s))
            .toList();
      }

      Object roles = jwt.getClaims().get("roles");
      if (roles instanceof Collection<?> c) {
        return c.stream().filter(String.class::isInstance).map(String.class::cast)
        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
            .toList();
      }

      return List.of();
    }
  }

  @Bean
  @ConditionalOnProperty(prefix = "app.security.jwt.hs256", name = "enabled", havingValue = "true", matchIfMissing = true)
  public JwtDecoder jwtDecoder(SecurityProperties securityProperties) {
    String secret = securityProperties.getJwt().getHs256().getSecret();
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException("app.security.jwt.hs256.secret must be at least 32 characters");
    }
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }
}
