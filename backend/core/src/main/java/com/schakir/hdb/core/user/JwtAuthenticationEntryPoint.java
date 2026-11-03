package com.schakir.hdb.core.user;

import com.schakir.hdb.core.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 401 responses (ADR 0037, D-038): the {@code WWW-Authenticate} header (RFC 6750 §3) is set here,
 * not by {@code BearerTokenAuthenticationEntryPoint}, which always appends {@code
 * error_description} and {@code resource_metadata} — a leak of the rejection reason the body is
 * deliberately generic about. Exactly {@code Bearer} with no token, {@code Bearer
 * error="invalid_token"} with a rejected one: a rejected token fails inside {@code
 * BearerTokenAuthenticationFilter} with an {@link OAuth2AuthenticationException}; a missing one
 * never reaches that filter's catch block and is denied downstream with a plain {@link
 * AuthenticationException}. The reason itself is logged at INFO, never the token, and never sent to
 * the caller.
 */
@Component
class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

  private final ObjectMapper mapper;

  JwtAuthenticationEntryPoint(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    log.info("Rejected request to {}: {}", request.getRequestURI(), exception.getMessage());
    String wwwAuthenticate =
        exception instanceof OAuth2AuthenticationException
            ? "Bearer error=\"invalid_token\""
            : "Bearer";
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate);
    ProblemDetailFactory.writeTo(
        response, mapper, HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized", null);
  }
}
