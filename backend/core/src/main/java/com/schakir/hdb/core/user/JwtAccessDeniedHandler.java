package com.schakir.hdb.core.user;

import com.schakir.hdb.core.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 403 responses (ADR 0037, D-038): delegates to {@link BearerTokenAccessDeniedHandler} for the
 * status and the {@code WWW-Authenticate: Bearer error="insufficient_scope"} header (RFC 6750 §3),
 * then writes the {@code /problems/forbidden} body. Never {@code response.reset()}: that would
 * erase the header the delegate just set.
 */
@Component
class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final BearerTokenAccessDeniedHandler delegate = new BearerTokenAccessDeniedHandler();
  private final ObjectMapper mapper;

  JwtAccessDeniedHandler(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException {
    delegate.handle(request, response, exception);
    ProblemDetailFactory.writeTo(
        response, mapper, HttpStatus.FORBIDDEN, "forbidden", "Forbidden", null);
  }
}
