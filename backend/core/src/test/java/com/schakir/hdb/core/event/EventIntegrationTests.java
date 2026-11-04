package com.schakir.hdb.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.schakir.hdb.core.event.testsupport.EventPublicationRecordingListener;
import com.schakir.hdb.testsupport.SecurityTestSupport;
import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Database behaviour of {@code event} (ADR 0028, ADR 0039, ADR 0040), through {@link EventService}
 * directly: same package as the module, so these tests reach its package-private types without
 * going through HTTP.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EventIntegrationTests extends SecurityTestSupport {

  @Autowired private EventService eventService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private EventPublicationRecordingListener recordingListener;

  @BeforeEach
  void clearRecordedPublications() {
    recordingListener.clear();
  }

  @Test
  void createsOneEventRowAndOnePublication() {
    Instant saleStart = Instant.now().plus(Duration.ofDays(1));
    Instant eventDate = saleStart.plus(Duration.ofDays(30));

    EventRecord created =
        eventService.createEvent(
            "Live Concert", "An unforgettable concert.", 150, saleStart, eventDate, 300);

    assertThat(countEvents(created.id())).isEqualTo(1);
    // Async, after commit (the registry's own mechanism, D-013/D-020): await it, never sleep.
    await()
        .untilAsserted(
            () ->
                assertThat(recordingListener.received())
                    .anyMatch(event -> event.eventId().equals(created.id())));
    assertThat(countPublicationsMentioning(created.id())).isGreaterThanOrEqualTo(1);
  }

  @Test
  void rejectsASaleStartTimeJustBelowTheMarginAndInsertsNothing() {
    Instant nowFromDatabase = now();
    Instant justBelowMargin = nowFromDatabase.plus(Duration.ofHours(1)).minusSeconds(5);
    Instant eventDate = justBelowMargin.plus(Duration.ofDays(1));
    long eventsBefore = countAllEvents();
    long publicationsBefore = countAllPublications();

    assertThatThrownBy(
            () -> eventService.createEvent("Too Soon", null, 10, justBelowMargin, eventDate, 300))
        .isInstanceOf(EventValidationException.class);

    assertThat(countAllEvents()).isEqualTo(eventsBefore);
    assertThat(countAllPublications()).isEqualTo(publicationsBefore);
  }

  @Test
  void acceptsASaleStartTimeComfortablyAboveTheMargin() {
    Instant nowFromDatabase = now();
    // A full extra minute above the 1h margin (application.yml): a buffer against this test's
    // own duration and CI slowness, since the insert's own now() runs later than this read.
    Instant justAboveMargin = nowFromDatabase.plus(Duration.ofHours(1)).plusSeconds(60);
    Instant eventDate = justAboveMargin.plus(Duration.ofDays(1));

    EventRecord created =
        eventService.createEvent("Comfortable Margin", null, 10, justAboveMargin, eventDate, 300);

    assertThat(countEvents(created.id())).isEqualTo(1);
  }

  @Test
  void databaseCheckRejectsASaleStartTimeNotBeforeTheEventDate() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO event.event
                      (id, name, capacity, sale_start_time, event_date, hold_duration_seconds)
                    VALUES (?, 'Direct insert', 10, now() + interval '1 day', now(), 300)
                    """,
                    UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void uuidv7IdsIncreaseAcrossSuccessiveInsertsOnTheSameConnection() {
    // uuidv7() guarantees monotonic order only within one PostgreSQL session (PostgreSQL 18):
    // both inserts must run on the same connection, so both createEvent calls are wrapped in
    // one outer transaction that the service's own @Transactional(REQUIRED) joins.
    List<EventRecord> created =
        transactionTemplate.execute(
            status -> List.of(createWithDefaults("First"), createWithDefaults("Second")));

    assertThat(created.get(1).id()).isGreaterThan(created.get(0).id());
  }

  private EventRecord createWithDefaults(String name) {
    Instant saleStart = Instant.now().plus(Duration.ofDays(1));
    return eventService.createEvent(name, null, 10, saleStart, saleStart.plusSeconds(3600), 300);
  }

  private Instant now() {
    return jdbcTemplate.queryForObject("SELECT now()", Timestamp.class).toInstant();
  }

  private long countAllEvents() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM event.event", Long.class);
  }

  private long countEvents(UUID id) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM event.event WHERE id = ?", Long.class, id);
  }

  private long countAllPublications() {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM modulith.event_publication", Long.class);
  }

  private long countPublicationsMentioning(UUID id) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM modulith.event_publication WHERE serialized_event LIKE ?",
        Long.class,
        "%" + id + "%");
  }
}
