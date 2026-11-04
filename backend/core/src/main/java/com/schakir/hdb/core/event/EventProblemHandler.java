package com.schakir.hdb.core.event;

import com.schakir.hdb.core.web.ProblemDetailFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps {@code event}'s own business problems (ADR 0037): ordered ahead of the shared {@code
 * TechnicalProblemHandler} ({@code LOWEST_PRECEDENCE}), so these take priority.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class EventProblemHandler {

  @ExceptionHandler(EventValidationException.class)
  ResponseEntity<Object> handleValidation(EventValidationException ex) {
    return ProblemDetailFactory.response(
        HttpStatus.BAD_REQUEST, "validation-error", "Invalid request", ex.detail());
  }

  @ExceptionHandler(EventNotFoundException.class)
  ResponseEntity<Object> handleNotFound(EventNotFoundException ex) {
    // No detail (matches the contract's own example): never echo the raw path input (D-037).
    return ProblemDetailFactory.response(
        HttpStatus.NOT_FOUND, "event-not-found", "Event not found", null);
  }
}
