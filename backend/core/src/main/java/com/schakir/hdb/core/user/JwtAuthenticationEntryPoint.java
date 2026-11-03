package com.schakir.hdb.core.user;

import com.schakir.hdb.core.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 401 responses (ADR 0037, D-038): delegates to {@link BearerTokenAuthenticationEntryPoint} for the
 * status and the {@code WWW-Authenticate} header (RFC 6750 §3) — {@code Bearer} with no token,
 * {@code Bearer error="invalid_token"} with a rejected one — then writes the {@code
 * /problems/unauthorized} body. Never {@code response.reset()}: that would erase the header the
 * delegate just set. The body never says why the token was rejected; the reason is logged at INFO,
 * never the token itself.
 */
@Component
class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

  private final BearerTokenAuthenticationEntryPoint delegate =
      new BearerTokenAuthenticationEntryPoint();
  private final ObjectMapper mapper;

  JwtAuthenticationEntryPoint(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    log.info("Rejected request to {}: {}", request.getRequestURI(), exception.getMessage());
    delegate.commence(request, response, exception);
    ProblemDetailFactory.writeTo(
        response, mapper, HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized", null);
  }
}
