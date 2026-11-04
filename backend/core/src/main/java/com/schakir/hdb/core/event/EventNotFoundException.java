package com.schakir.hdb.core.event;

import java.util.UUID;

/** No event with the given id (ADR 0037): maps to 404 {@code /problems/event-not-found}. */
final class EventNotFoundException extends RuntimeException {

  EventNotFoundException(UUID eventId) {
    super("No event with id " + eventId);
  }
}
