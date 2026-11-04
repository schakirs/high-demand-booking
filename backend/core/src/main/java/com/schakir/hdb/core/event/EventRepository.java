package com.schakir.hdb.core.event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain SQL with {@code JdbcClient} (ADR 0040), schema-qualified into {@code event} (D-033). No
 * Spring Data, no JPA.
 */
@Repository
class EventRepository {

  private final JdbcClient jdbcClient;

  EventRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /**
   * Enforces the sale-opening margin inside the insert itself, as a single conditional statement
   * (ADR 0027, ADR 0028, ADR 0040): an empty result means the margin was violated, never a thrown
   * exception from SQL. {@code now()} is PostgreSQL's, never the JVM clock.
   *
   * <p>{@code RETURNING} every stored column, mapped by the same {@link #mapRow} as {@link
   * #findById}, rather than echoing back the method's own parameters: PostgreSQL stores microsecond
   * precision, so the row actually written — not the {@link Instant} passed in — is what the 201
   * response must show.
   */
  Optional<EventRecord> insert(
      String name,
      String description,
      int capacity,
      Instant saleStartTime,
      Instant eventDate,
      int holdDurationSeconds,
      Duration margin) {
    return jdbcClient
        .sql(
            """
            INSERT INTO event.event
              (name, description, capacity, sale_start_time, event_date, hold_duration_seconds)
            SELECT :name, :description, :capacity, :saleStartTime, :eventDate, :holdDurationSeconds
            WHERE :saleStartTime > now() + make_interval(secs => :marginSeconds)
            RETURNING id, name, description, capacity, sale_start_time, event_date,
                      hold_duration_seconds, created_at
            """)
        .param("name", name)
        .param("description", description)
        .param("capacity", capacity)
        .param("saleStartTime", Timestamp.from(saleStartTime))
        .param("eventDate", Timestamp.from(eventDate))
        .param("holdDurationSeconds", holdDurationSeconds)
        .param("marginSeconds", margin.getSeconds())
        .query(EventRepository::mapRow)
        .optional();
  }

  Optional<EventRecord> findById(UUID id) {
    return jdbcClient
        .sql(
            """
            SELECT id, name, description, capacity, sale_start_time, event_date,
                   hold_duration_seconds, created_at
            FROM event.event
            WHERE id = :id
            """)
        .param("id", id)
        .query(EventRepository::mapRow)
        .optional();
  }

  private static EventRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new EventRecord(
        rs.getObject("id", UUID.class),
        rs.getString("name"),
        rs.getString("description"),
        rs.getInt("capacity"),
        rs.getTimestamp("sale_start_time").toInstant(),
        rs.getTimestamp("event_date").toInstant(),
        Duration.ofSeconds(rs.getInt("hold_duration_seconds")),
        rs.getTimestamp("created_at").toInstant());
  }
}
