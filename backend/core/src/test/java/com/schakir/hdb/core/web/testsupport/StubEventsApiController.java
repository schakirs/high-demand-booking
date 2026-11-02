package com.schakir.hdb.core.web.testsupport;

import com.schakir.hdb.core.event.api.EventsApi;
import com.schakir.hdb.core.event.api.model.CreateEventRequest;
import com.schakir.hdb.core.event.api.model.Event;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the real generated {@code EventsApi} (stub body, never in {@code src/main}): proves
 * that the generated Bean Validation annotations (not just a hand-written {@code @Valid}) flow into
 * {@code TechnicalProblemHandler} and produce {@code /problems/validation-error}.
 */
@RestController
class StubEventsApiController implements EventsApi {

  @Override
  public ResponseEntity<Event> createEvent(CreateEventRequest request) {
    Event event =
        new Event(
            UUID.randomUUID(),
            request.getName(),
            request.getCapacity(),
            request.getSaleStartTime(),
            request.getEventDate(),
            request.getHoldDurationSeconds(),
            Instant.now());
    event.setDescription(request.getDescription());
    return ResponseEntity.status(201).body(event);
  }

  @Override
  public ResponseEntity<Event> getEvent(UUID eventId) {
    return ResponseEntity.notFound().build();
  }
}
