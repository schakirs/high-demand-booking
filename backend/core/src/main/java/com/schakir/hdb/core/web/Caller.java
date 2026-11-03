package com.schakir.hdb.core.web;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads the authenticated caller's id (D-038, CLAUDE.md rule 2): generated API interfaces have a
 * fixed signature, so a controller cannot take an {@code @AuthenticationPrincipal Jwt} parameter
 * and must read the security context instead. Fails hard (an unchecked exception, becoming a 500)
 * if called without an authenticated JWT; every reachable operation requires one (CLAUDE.md rule
 * 10), so that should never happen in practice.
 */
public final class Caller {

  private Caller() {}

  public static UUID id() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication == null ? null : authentication.getPrincipal();
    if (!(principal instanceof Jwt jwt)) {
      throw new IllegalStateException("Caller.id() requires an authenticated JWT");
    }
    return UUID.fromString(jwt.getSubject());
  }
}
