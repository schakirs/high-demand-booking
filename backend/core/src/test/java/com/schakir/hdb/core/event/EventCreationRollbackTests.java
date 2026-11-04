package com.schakir.hdb.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schakir.hdb.core.event.testsupport.TransactionRollbackSimulatingConfiguration;
import com.schakir.hdb.core.event.testsupport.TransactionRollbackSimulatingConfiguration.FailureInjectingListener;
import com.schakir.hdb.testsupport.SecurityTestSupport;
import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Same-transaction guarantee (ADR 0027, ADR 0039): a failure after {@code EventService#createEvent}
 * calls {@code ApplicationEventPublisher#publishEvent}, but before commit, must roll back both the
 * event row and the registry's publication row together.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TransactionRollbackSimulatingConfiguration.class})
class EventCreationRollbackTests extends SecurityTestSupport {

  @Autowired private EventService eventService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private FailureInjectingListener failureInjectingListener;

  @Test
  void aFailureAfterPublishEventRollsBackBothTheEventAndThePublicationRow() {
    Instant saleStart = Instant.now().plus(Duration.ofDays(1));
    Instant eventDate = saleStart.plus(Duration.ofDays(30));

    assertThatThrownBy(
            () -> eventService.createEvent("Rolled Back", null, 10, saleStart, eventDate, 300))
        .hasMessageContaining("simulated failure");

    // Proves the rollback assertion below is meaningful: the row really existed, uncommitted,
    // before the failure (see the listener's own Javadoc).
    assertThat(failureInjectingListener.countsBeforeFailure()).containsExactly(1L, 1L);

    assertThat(countAllEvents()).isZero();
    assertThat(countAllPublications()).isZero();
  }

  private long countAllEvents() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM event.event", Long.class);
  }

  private long countAllPublications() {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM modulith.event_publication", Long.class);
  }
}
