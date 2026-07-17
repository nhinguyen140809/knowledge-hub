package com.knowledgehub.access.infrastructure.security;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import com.knowledgehub.access.domain.port.Authenticator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Stateless token security for the whole application. There are no sessions, CSRF tokens or login
 * forms — this is a bearer-token API. Health and API docs are public; every other request, REST and
 * {@code /mcp} alike, must authenticate. A {@link BearerAuthFilter} runs before authorization to
 * set the principal, and unauthenticated/forbidden responses share the application's problem-detail
 * shape. {@code @EnableMethodSecurity} turns on {@code @PreAuthorize} for admin endpoints.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig {

  /**
   * The single filter chain every HTTP request passes through, in order: {@link BearerAuthFilter}
   * settles who the caller is, then the URL rules decide whether that is enough ({@code permitAll}
   * for health and API docs, {@code authenticated} for everything else), then method security
   * checks roles on admin endpoints. Both failure kinds — unauthenticated and forbidden — are
   * rendered by the {@link ProblemSecurityHandler} so they match the application's error schema.
   *
   * <p>Example: {@code GET /actuator/health} passes with no token; {@code POST /api/v1/query} needs
   * a valid bearer token; {@code POST /api/v1/admin/sources} additionally needs the token's
   * principal to have the admin role.
   */
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, BearerAuthFilter bearerAuthFilter, ProblemSecurityHandler problemHandler)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health/**", "/docs/**", "/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(bearerAuthFilter, AuthorizationFilter.class)
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(problemHandler).accessDeniedHandler(problemHandler));
    return http.build();
  }

  /**
   * The authentication filter, declared as a bean here instead of being component-scanned so it
   * only exists together with the security configuration that places it in the chain.
   */
  @Bean
  BearerAuthFilter bearerAuthFilter(
      Authenticator authenticator, ProblemSecurityHandler problemHandler) {
    return new BearerAuthFilter(authenticator, problemHandler);
  }
}
