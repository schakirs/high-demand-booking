package com.schakir.hdb.core.event;

/** A business validation rule rejected the request (ADR 0037, ADR 0040): maps to 400. */
final class EventValidationException extends RuntimeException {

  private final String field;

  EventValidationException(String field, String message) {
    super(message);
    this.field = field;
  }

  String field() {
    return field;
  }

  String detail() {
    return field + ": " + getMessage();
  }
}
