package com.schakir.hdb.core.user;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The security filter chain (D-038, CLAUDE.md rule 10): every contract operation has an explicit
 * URL rule; anything not listed is denied (fail closed, {@code anyRequest().denyAll()}). An
 * anonymous caller denied by {@code denyAll()}/{@code authenticated()} gets 401 (not yet
 * authenticated); a fully authenticated caller denied by {@code hasAuthority(...)} gets 403 — that
 * distinction is Spring Security's own {@code ExceptionTranslationFilter} behaviour, not custom
 * logic here.
 *
 * <p>{@code /test-only/**} is test scaffolding (exists only under {@code src/test}, never reachable
 * in production) exempted so the shared technical error handling tests (#5) keep running without a
 * token. {@code DispatcherType.ERROR} is exempted so a container error dispatch (Boot's {@code
 * /error} forward for an unmapped path or a raw {@code sendError}) is never re-authorized into a
 * 401/403.
 *
 * <p>No {@code @PreAuthorize}: an {@code AccessDeniedException} raised inside MVC would be turned
 * into a 500 by {@code TechnicalProblemHandler}, not handled here.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationEntryPoint authenticationEntryPoint,
      JwtAccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/test-only/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/v1/events")
                    .hasAuthority("SCOPE_admin")
                    .requestMatchers(HttpMethod.GET, "/v1/events/{eventId}")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/v1/events/{eventId}/holds")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/v1/holds/{holdId}")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/v1/holds/{holdId}/confirmations")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer
                    .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler));
    return http.build();
  }
}
