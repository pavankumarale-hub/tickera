# ADR 0002 — Database per service

- Status: Accepted
- Date: 2026-07

## Context

Three services each maintain state: booking (event store + read model), payment
(event store + read model), notification (a simple table). We must decide whether
they share a database or each owns one.

## Decision

Each service owns a **private PostgreSQL database** (`postgres-booking`,
`postgres-payment`, `postgres-notification`). No service reads or writes another
service's tables. The only cross-service coupling is the Kafka integration-event
contract.

## Rationale

- **Independent schema evolution.** booking-service can add tables or reshape its
  read model without a coordinated migration across teams.
- **Failure isolation.** A slow or locked payment database cannot degrade booking
  queries.
- **Clear ownership.** The event store *is* the service's private history;
  exposing it via a shared DB would let others depend on internal events.
- **Scales the pattern honestly.** It is the realistic microservices shape a
  reviewer expects to see, and it makes the "integrate only through events"
  discipline unavoidable rather than optional.

## Consequences

- No cross-service joins or distributed transactions. Consistency across services
  is achieved with events and the saga (ADR-0004), not 2PC.
- Reference data needed by multiple services is replicated via events, not shared.
- More containers to run locally — acceptable, and Docker Compose hides it behind
  one command.

## Alternatives considered

- **Shared database, separate schemas:** less infra but reintroduces coupling and
  tempts cross-schema queries; rejected.
- **One database, one service each connecting:** same coupling risk; rejected.
