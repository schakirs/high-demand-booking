package com.schakir.hdb.core.user;

import com.schakir.hdb.core.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 403 responses (ADR 0037, D-038): the {@code WWW-Authenticate: Bearer error="insufficient_scope"}
 * header (RFC 6750 §3) is set here, not by {@code BearerTokenAccessDeniedHandler}, which also
 * appends {@code error_description} and {@code resource_metadata}.
 */
@Component
class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper mapper;

  JwtAccessDeniedHandler(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\"");
    ProblemDetailFactory.writeTo(
        response, mapper, HttpStatus.FORBIDDEN, "forbidden", "Forbidden", null);
  }
}
