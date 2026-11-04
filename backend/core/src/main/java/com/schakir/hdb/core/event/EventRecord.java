package com.schakir.hdb.core.event;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The event module's own representation of a row in {@code event.event}: the repository and the
 * service use this, never the generated {@code event.api.model.Event} (a contract change must not
 * ripple into the SQL code). The controller maps both ways.
 */
record EventRecord(
    UUID id,
    String name,
    String description,
    int capacity,
    Instant saleStartTime,
    Instant eventDate,
    Duration holdDuration,
    Instant createdAt) {}
