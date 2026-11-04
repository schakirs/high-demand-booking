package com.schakir.hdb.core.event.testsupport;

import com.schakir.hdb.core.event.internal.EventCreated;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Records {@code EventCreated} publications so integration tests can assert on them (decision 6 of
 * #7/#12): no production listener exists yet (#8 adds {@code inventory.createStock}).
 * {@code @ApplicationModuleListener} is async, after commit, by composition (D-013, D-020): a test
 * awaits it with Awaitility, never {@code Thread.sleep}. Only an {@code @ApplicationModuleListener}
 * (or {@code @TransactionalEventListener}) gets a row in {@code modulith.event_publication} (a
 * plain {@code @EventListener} does not), so this is also what proves the registry records the
 * publication at all.
 */
@Component
public class EventPublicationRecordingListener {

  private final List<EventCreated> received = new CopyOnWriteArrayList<>();

  @ApplicationModuleListener
  void on(EventCreated event) {
    received.add(event);
  }

  public List<EventCreated> received() {
    return received;
  }

  public void clear() {
    received.clear();
  }
}
