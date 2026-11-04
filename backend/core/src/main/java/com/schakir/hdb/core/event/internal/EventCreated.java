package com.schakir.hdb.core.event.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Published after an event is inserted, in the same transaction (ADR 0027, ADR 0039). Internal to
 * {@code event} in iteration 1: only {@code event} itself listens, after commit, to call {@code
 * inventory.createStock(...)} (#8).
 *
 * <p>Stored publications are replayed from their serialized JSON (D-013): fields may be added, but
 * never renamed or removed.
 */
public record EventCreated(UUID eventId, int capacity, Instant opensAt, Duration holdDuration) {}
