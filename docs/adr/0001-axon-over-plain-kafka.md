# ADR 0001 — Axon for in-service CQRS/ES, Kafka for inter-service integration

- Status: Accepted
- Date: 2026-07

## Context

The system needs (a) an auditable, replayable history of what happened to each
booking and payment, with enforced state-transition invariants, and (b) durable,
decoupled communication between independently deployable services.

A common junior instinct is to use one tool for both: either "Kafka everywhere"
(services publish/consume raw Kafka topics and keep state in a CRUD database) or
"Axon everywhere" (Axon Server as the only bus). Both conflate two different
concerns — *intra-service consistency* and *inter-service transport*.

## Decision

Use **Axon Framework inside each service** for the command/event/aggregate/saga
model and event sourcing, and use **Kafka between services** for integration
events.

- Axon owns aggregates, the event store, projections, sagas, and deadlines.
- Kafka carries a small set of versioned integration events (`common-events`).
- A thin anti-corruption layer in each service translates internal domain events
  to/from the public Kafka contracts.

We run Axon **without Axon Server**: the event store and tracking tokens live in
the service-local Postgres. This keeps the infra footprint to Postgres + Kafka +
Redis and reinforces database-per-service.

## Why Axon over hand-rolled Kafka consumers (for in-service logic)

- **Invariants in one place.** The aggregate is the transactional boundary;
  illegal transitions throw before any event is stored. With plain Kafka you
  reimplement optimistic locking and validation per consumer.
- **Event sourcing for free.** Append-only store, replay, and projection rebuild
  are first-class. Rebuilding a read model is a token reset, not a migration.
- **Sagas + deadlines.** Cross-step orchestration with timeouts and compensation
  is a framework primitive, not bespoke scheduler code.
- **Testability.** `AggregateTestFixture` gives given/when/then tests over the
  event stream with no infrastructure.

## Why Kafka over Axon Server (for between services)

- **Ubiquity & ops maturity** in most orgs; strong partitioning/retention story.
- **Replayable log** decoupled from any one service's lifecycle.
- **No single framework coupling** across service boundaries — a non-Axon
  consumer (notification-service) integrates with nothing but the JSON contract.

## Consequences

- Two messaging mental models coexist; the ACL boundary must be explicit and
  tested (hence the Pact message contracts).
- Eventual consistency across services is intrinsic — addressed in ADR-0004.
- Losing Axon Server means no out-of-the-box distributed command routing; not
  needed here since aggregates are owned by a single service.
