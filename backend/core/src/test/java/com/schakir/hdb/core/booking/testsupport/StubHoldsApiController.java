package com.schakir.hdb.core.booking.testsupport;

import com.schakir.hdb.core.booking.api.HoldsApi;
import com.schakir.hdb.core.booking.api.model.Hold;
import com.schakir.hdb.core.booking.api.model.HoldStatus;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Implements the real generated {@code HoldsApi} (stub body, never in {@code src/main}): gives the
 * five contract operations something to route to for the security tests (#6), mirroring the
 * temporary {@code StubEventsApiController} pattern from #5, until the real controller lands (#7).
 *
 * <p>{@link #getHold} doubles as the caller-id probe: it returns the token's {@code sub} as {@code
 * Hold.id}, read from the security context (the generated interface's fixed signature has no room
 * for an {@code @AuthenticationPrincipal Jwt} parameter) — proving the caller id comes only from
 * the token, never from a header or query parameter a caller might also send.
 *
 * <p>{@link #confirmHold} doubles as the {@code /error}-dispatch probe: it calls {@code
 * response.sendError(500)} directly and returns {@code null} (Spring MVC's signal that the response
 * was already written), so the container performs a genuine ERROR-dispatch forward to {@code
 * /error} — proving that forward is not re-authorized into a 401/403.
 */
@RestController
class StubHoldsApiController implements HoldsApi {

  @Override
  public ResponseEntity<Hold> createHold(UUID eventId) {
    return ResponseEntity.notFound().build();
  }

  @Override
  public ResponseEntity<Hold> getHold(UUID holdId) {
    Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    UUID callerId = UUID.fromString(jwt.getSubject());
    Hold hold = new Hold(callerId, holdId, HoldStatus.ACTIVE, Instant.now().plusSeconds(60));
    return ResponseEntity.ok(hold);
  }

  @Override
  public ResponseEntity<Hold> confirmHold(UUID holdId) {
    HttpServletResponse response =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
    try {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return null;
  }
}
