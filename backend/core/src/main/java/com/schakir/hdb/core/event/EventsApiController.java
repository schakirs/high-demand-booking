package com.schakir.hdb.core.event;

import com.schakir.hdb.core.event.api.EventsApi;
import com.schakir.hdb.core.event.api.model.CreateEventRequest;
import com.schakir.hdb.core.event.api.model.Event;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the generated {@code EventsApi} (ADR 0036). The admin's id is never read (not in the
 * contract, decision 8 of #7/#12): no call to {@code Caller.id()}.
 */
@RestController
class EventsApiController implements EventsApi {

  private final EventService eventService;

  EventsApiController(EventService eventService) {
    this.eventService = eventService;
  }

  @Override
  public ResponseEntity<Event> createEvent(CreateEventRequest request) {
    EventRecord event =
        eventService.createEvent(
            request.getName(),
            request.getDescription(),
            request.getCapacity(),
            request.getSaleStartTime(),
            request.getEventDate(),
            request.getHoldDurationSeconds());

    // Relative reference (decision 7 of #7/#12): no host built from the request, so no
    // dependency on forwarded headers.
    return ResponseEntity.created(URI.create("/v1/events/" + event.id())).body(toModel(event));
  }

  @Override
  public ResponseEntity<Event> getEvent(UUID eventId) {
    return ResponseEntity.ok(toModel(eventService.getEvent(eventId)));
  }

  private static Event toModel(EventRecord event) {
    Event model =
        new Event(
            event.id(),
            event.name(),
            event.capacity(),
            event.saleStartTime(),
            event.eventDate(),
            (int) event.holdDuration().getSeconds(),
            event.createdAt());
    model.setDescription(event.description());
    return model;
  }
}
