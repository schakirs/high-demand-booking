# Architecture Decision Records

One file per decision (D-026). ADR `00NN` records decision `D-0NN` of the project reference.
Template: `0000-template.md`. ADRs 0001–0026 were converted from the reference on 2026-10-25:
options are listed only when the recorded rationale names the alternative it rejects;
"Not recorded" means the reference keeps no alternative.
"Candidate signal" in *Revisit when* marks a signal added during conversion.

An accepted ADR is not rewritten. A change is a new ADR that amends (partial change) or
supersedes (full replacement) an earlier one; only the earlier ADR's Status line is updated.
This rule applies from the first commit of the ADRs (ticket 1).

| ADR | Title | Status |
|---|---|---|
| [0001](0001-learning-over-commercial-goals.md) | Learning comes before any commercial goal | Accepted |
| [0002](0002-integrity-over-availability.md) | Integrity before availability on the booking path | Accepted |
| [0003](0003-client-generated-idempotency-keys.md) | Assume retries: client-generated idempotency keys | Accepted, amended by 0023 (holds use a business key) |
| [0004](0004-concrete-domain-language.md) | Concrete domain language; only the inventory is generic | Accepted |
| [0005](0005-waiting-room-always-on.md) | The waiting room is always active in the MVP | Accepted |
| [0006](0006-lottery-at-opening-then-fifo.md) | Lottery at sale opening, then FIFO | Accepted |
| [0007](0007-minimal-identity.md) | Minimal identity: test users and signed tokens | Accepted, amended by 0030 (mechanism) |
| [0008](0008-grace-period-before-release.md) | Decouple visible expiry from actual release (grace period) | Accepted |
| [0009](0009-separate-read-and-booking-paths.md) | Separate the position read path from the booking path (CQRS) | Accepted |
| [0010](0010-hold-owned-by-inventory.md) | Holds belong to the inventory; atomic hold-to-booking conversion | Accepted |
| [0011](0011-asynchronous-notification.md) | Asynchronous notification, outside the critical path, with retries | Accepted |
| [0012](0012-scheduler-publishes-when.md) | The scheduler publishes the "when"; components execute the "what" | Accepted |
| [0013](0013-transactional-outbox.md) | Publish events through a transactional outbox | Accepted, amended by 0020 (the outbox is the Spring Modulith registry) |
| [0014](0014-position-from-admission-cursor.md) | Position = rank − admission cursor, served by cache and CDN | Accepted |
| [0015](0015-idempotent-payment-calls.md) | Idempotent payment calls, reconciliation, idempotent webhooks | Accepted |
| [0016](0016-inventory-in-three-stages.md) | Inventory in three stages, starting with a PostgreSQL conditional update | Accepted |
| [0017](0017-modular-monolith-plus-two-deployables.md) | Modular monolith plus two separate deployables | Accepted |
| [0018](0018-trunk-based-development.md) | Trunk-based development, one artefact promoted across environments | Accepted |
| [0019](0019-kanban-on-github.md) | Kanban on GitHub Issues and Projects, WIP limit 2 | Accepted |
| [0020](0020-spring-modulith.md) | Spring Modulith for module boundaries and as the outbox | Accepted |
| [0021](0021-contract-first-api.md) | Contract-first API, Problem Details errors, version in the URL | Accepted |
| [0022](0022-technology-stack.md) | Technology stack | Accepted |
| [0023](0023-hold-idempotent-by-business-key.md) | Holds are idempotent by business key (event, user) | Accepted |
| [0024](0024-booking-api-as-projection.md) | Booking exposes the API; its state will be fed by inventory events | Accepted |
| [0025](0025-openapi-conventions.md) | OpenAPI 3.1.1, snake_case, ISO 8601 UTC; read a hold | Accepted |
| [0026](0026-iteration-1-local-and-tooling.md) | Iteration 1 local and in CI only; tooling; one ADR per decision | Accepted |
| [0027](0027-dependency-direction-inventory-depends-on-nothing.md) | Dependency direction: inventory depends on no module | Accepted |
| [0028](0028-sale-opening-enforced-by-inventory.md) | Sale opening enforced by inventory, PostgreSQL clock as time source | Accepted |
| [0029](0029-distinguish-sold-out-from-temporarily-unavailable.md) | Distinguish "sold out" from "temporarily unavailable" from iteration 1 | Accepted |
| [0030](0030-simulated-identity.md) | Simulated identity with locally signed bearer tokens | Accepted |
| [0031](0031-hold-expiry-by-inventory-sweep.md) | Hold expiry by a periodic sweep owned by inventory | Accepted |
