-- Spring Modulith's event publication registry (D-013, D-020), the transactional outbox
-- relay. Schema-qualified into its own schema, never `public` (ADR 0039, amends D-033):
-- this table is shared infrastructure, not one module's data.
--
-- DDL copied from spring-modulith-events-jdbc 2.1.2's own
-- org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql, schema-qualified
-- (`IF NOT EXISTS` dropped: this is a fresh migration, never re-run). Compare against that
-- file on every Modulith upgrade (ADR 0039).
CREATE SCHEMA modulith;

CREATE TABLE modulith.event_publication (
    id                     uuid NOT NULL,
    listener_id            text NOT NULL,
    event_type             text NOT NULL,
    serialized_event       text NOT NULL,
    publication_date       timestamptz NOT NULL,
    completion_date        timestamptz,
    status                 text,
    completion_attempts    integer,
    last_resubmission_date timestamptz,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx
    ON modulith.event_publication USING hash (serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
    ON modulith.event_publication (completion_date);
