package com.schakir.hdb.core.web.testsupport;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exercises the shared technical error handling (ADR 0037) end to end. Not a contract endpoint: it
 * exists only under {@code src/test} to drive {@code TechnicalProblemHandler} with a validated body
 * and a UUID path parameter.
 */
@RestController
@RequestMapping("/test-only/samples")
class SampleValidatedController {

  record SampleRequest(@NotBlank String name) {}

  @PostMapping("/{id}")
  ResponseEntity<String> create(@PathVariable UUID id, @Valid @RequestBody SampleRequest body) {
    if ("boom".equals(body.name())) {
      throw new IllegalStateException("simulated failure, must never reach the response body");
    }
    return ResponseEntity.ok(body.name());
  }

  /** Drives a Spring MVC exception this handler does not override: a missing parameter. */
  @GetMapping("/by-name")
  ResponseEntity<String> byName(@RequestParam String n) {
    return ResponseEntity.ok(n);
  }

  /** Drives a raw {@code ResponseStatusException}, also not overridden by this handler. */
  @GetMapping("/conflict")
  ResponseEntity<String> conflict() {
    throw new ResponseStatusException(HttpStatus.CONFLICT, "already in progress");
  }
}
