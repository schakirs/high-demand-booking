package com.schakir.hdb.core.event.testsupport;

import com.schakir.hdb.core.event.internal.EventCreated;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Injects a failure after {@code EventService} calls {@code
 * ApplicationEventPublisher#publishEvent}, but before the surrounding transaction commits: proves
 * the "one transaction" guarantee of ADR 0027/0039 (the event row and the registry's publication
 * row must both disappear together on rollback). Imported only by the one test that needs it
 * ({@code EventCreationRollbackTests}), so it never affects any other test.
 */
@TestConfiguration
public class TransactionRollbackSimulatingConfiguration {

  @Bean
  FailureInjectingListener failureInjectingListener(JdbcTemplate jdbcTemplate) {
    return new FailureInjectingListener(jdbcTemplate);
  }

  /**
   * A plain, synchronous {@code @EventListener} (not {@code @ApplicationModuleListener}): runs in
   * the same thread and the same transaction as the insert, before commit.
   */
  public static class FailureInjectingListener {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicReference<long[]> countsBeforeFailure = new AtomicReference<>();

    FailureInjectingListener(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Reads both counts, on the same connection and transaction, before throwing. These counts —
     * not an assumption about listener ordering relative to the registry's own write — are what
     * make the rollback assertion meaningful: if the registry row had not actually been written yet
     * at this point, this would read 0 and the test fails loudly here, instead of passing vacuously
     * because the row was simply never written at all.
     */
    @EventListener(EventCreated.class)
    void onPublishedBeforeCommit(EventCreated event) {
      long eventRows = jdbcTemplate.queryForObject("SELECT count(*) FROM event.event", Long.class);
      long publicationRows =
          jdbcTemplate.queryForObject(
              "SELECT count(*) FROM modulith.event_publication", Long.class);
      countsBeforeFailure.set(new long[] {eventRows, publicationRows});
      throw new RuntimeException("simulated failure after publishEvent, before commit");
    }

    public long[] countsBeforeFailure() {
      return countsBeforeFailure.get();
    }
  }
}
