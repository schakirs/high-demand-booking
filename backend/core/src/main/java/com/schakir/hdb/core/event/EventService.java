package com.schakir.hdb.core.event;

import com.schakir.hdb.core.event.internal.EventCreated;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties(EventProperties.class)
class EventService {

  private final EventRepository eventRepository;
  private final EventProperties eventProperties;
  private final ApplicationEventPublisher eventPublisher;

  EventService(
      EventRepository eventRepository,
      EventProperties eventProperties,
      ApplicationEventPublisher eventPublisher) {
    this.eventRepository = eventRepository;
    this.eventProperties = eventProperties;
    this.eventPublisher = eventPublisher;
  }

  /**
   * One transaction for the insert and the publication (ADR 0027, ADR 0039): if anything after
   * {@link ApplicationEventPublisher#publishEvent} fails before commit, neither the event row nor
   * the publication row survives (Spring Modulith's registry writes within the same transaction).
   */
  @Transactional
  EventRecord createEvent(
      String name,
      String description,
      int capacity,
      Instant saleStartTime,
      Instant eventDate,
      int holdDurationSeconds) {
    if (!saleStartTime.isBefore(eventDate)) {
      throw new EventValidationException("sale_start_time", "must be before event_date");
    }

    EventRecord event =
        eventRepository
            .insert(
                name,
                description,
                capacity,
                saleStartTime,
                eventDate,
                holdDurationSeconds,
                eventProperties.saleOpeningMargin())
            .orElseThrow(
                () ->
                    new EventValidationException(
                        "sale_start_time",
                        "must be later than the current time plus the configured margin"));

    eventPublisher.publishEvent(
        new EventCreated(
            event.id(), event.capacity(), event.saleStartTime(), event.holdDuration()));

    return event;
  }

  EventRecord getEvent(UUID eventId) {
    return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
  }
}
