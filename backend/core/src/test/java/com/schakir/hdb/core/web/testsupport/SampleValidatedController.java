package com.schakir.hdb.core.web.testsupport;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
