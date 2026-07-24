# Low-Level Design — booking-service

> Status: Living document · Owner: Pavan Kumar · Scope: `booking-service`

This LLD documents the internal design of the booking service to the level a
reviewer or a new joiner needs before touching the code. It deliberately mirrors
the LLD artifacts I own at work: component responsibilities, the write/read
model split, sequence flows, failure handling, and the data model.

## 1. Responsibilities

`booking-service` is the system of record for the booking lifecycle. It:

1. Accepts booking commands over REST and turns them into event-sourced state
   transitions on the `BookingAggregate`.
2. Maintains a query-optimised read model (`BookingSummary`) via a projection.
3. Publishes the public `BookingConfirmed` integration event to Kafka.
4. Runs the `BookingSaga`, which orchestrates payment and enforces a payment
   timeout with compensation.
5. Reacts to payment outcomes (from Kafka) by issuing follow-up commands.

Out of scope: taking money (payment-service), sending messages (notification-service).

## 2. Component model

| Component | Type | Responsibility |
|-----------|------|----------------|
| `BookingController` | REST adapter | Maps HTTP to commands (`CommandGateway`) and queries (`BookingQueryService`) |
| `ApiExceptionHandler` | REST advice | Translates aggregate invariants to RFC-7807 4xx responses |
| `BookingAggregate` | Axon aggregate | Owns invariants; command → event; event → state |
| `BookingProjection` | Axon event/query handler | Builds `BookingSummary`; serves query messages |
| `BookingQueryService` | Service | Read facade with Redis read-through cache |
| `BookingIntegrationEventPublisher` | Axon event handler | Domain event → Kafka integration event (ACL) |
| `PaymentEventsConsumer` | Kafka listener | Payment integration events → booking commands (ACL) |
| `BookingSaga` | Axon saga | Orchestration + payment deadline + compensation |
| `AxonConfig` / `CacheConfig` | Config | DeadlineManager; Spring cache enablement |

`coreapi` (commands + events) is the module's published language, kept separate
from handlers so tests and the saga depend on contracts, not implementations.

## 3. State machine

```
CREATED --confirm--> CONFIRMED --pay--> PAID
   |                      |
   |                      +--cancel/timeout--> CANCELLED
   +----------cancel----------------------->  CANCELLED
```

Invariants enforced in `BookingAggregate`:

- `seats >= 1` and `amount > 0` at creation.
- `confirm` only from `CREATED`.
- `markPaid` only from `CONFIRMED`.
- `cancel` forbidden from `PAID` or `CANCELLED` (terminal states).

Because handlers `apply(...)` events and never mutate fields directly, an
illegal command throws *before* any event is written — the store only ever
contains valid transitions.

## 4. Write model (event sourcing)

- **Event store:** Axon's JPA event store in the service-local Postgres
  (`domain_event_entry`, `snapshot_event_entry`). No Axon Server (see ADR-0001).
- **Serialization:** Jackson for events/messages/tokens — events stay
  human-readable in the store and there is no XStream allow-list to maintain on
  Java 17.
- **Concurrency:** Axon uses optimistic locking on the aggregate's event
  sequence number; concurrent commands on the same booking are serialized.

## 5. Read model (CQRS)

- `BookingSummary` (table `booking_summary`) is a flat projection keyed by
  `bookingId`, updated by `BookingProjection` under processing group
  `booking-projection` (a tracking processor with its own token).
- `BookingQueryService.findById` is `@Cacheable("bookings")` against Redis;
  the projection `@CacheEvict`s on every state change, giving read-through
  caching with event-driven invalidation and a 5-minute TTL as a safety net.
- Rebuild: reset the `booking-projection` token to replay all events and
  regenerate the read model from scratch — a property only event sourcing gives.

## 6. Key flows

### 6.1 Confirm → payment (happy path)

1. `POST /bookings/{id}/confirm` → `ConfirmBookingCommand`.
2. Aggregate applies `BookingConfirmedEvent`.
3. Three handlers react to that event, each in its own processing group:
   - projection sets status `CONFIRMED`;
   - integration publisher emits `BookingConfirmedIntegrationEvent` to Kafka;
   - saga starts and schedules a `payment-timeout` deadline.
4. payment-service charges and emits `PaymentCompleted`.
5. `PaymentEventsConsumer` issues `MarkBookingPaidCommand` → `BookingPaidEvent`.
6. Saga cancels the deadline and ends.

### 6.2 Payment failure / timeout (compensation)

- On `PaymentFailed`, the consumer issues `CancelBookingCommand`.
- If no payment outcome arrives before the deadline, the saga's
  `@DeadlineHandler` issues `CancelBookingCommand("Payment not received in time")`.
- Either way the booking reaches `CANCELLED` and the saga ends — seats released.

## 7. Failure handling

| Failure | Handling |
|---------|----------|
| Illegal transition | Aggregate throws → `ApiExceptionHandler` → 409 Conflict |
| Bad input | Bean Validation / aggregate guard → 400 |
| Kafka publish transient failure | Isolated in `integration-publisher` processing group; a tracking processor retries from its token without affecting the projection |
| Duplicate payment event | Aggregate state guard makes `MarkBookingPaid` on a PAID booking a no-op |
| Lost payment reply | Saga deadline → compensation |

## 8. Data model (per-service Postgres)

- Axon-managed: `domain_event_entry`, `snapshot_event_entry`, `token_entry`,
  `saga_entry`, `association_value_entry`.
- Application-managed: `booking_summary` (read model).

Schema is managed by **Flyway** (`ddl-auto: none`). `V1__initial_schema.sql`
creates all Axon tables with their named sequences (required for Hibernate's
`SEQUENCE` generation strategy with `INCREMENT BY 50`) and the `booking_summary`
read-model table. Running `flyway:repair` then `flyway:migrate` is the standard
recovery path if schema state diverges.

## 9. Observability

- `/actuator/health`, `/actuator/prometheus`, `/actuator/metrics`.
- Micrometer tags every metric with `application=booking-service`; Prometheus
  scrapes and Grafana renders request rate, p95 latency, JVM heap, and Kafka
  consumption (see `infra/grafana/dashboards`).

## 10. Known gaps / next steps

- Swap `SimpleDeadlineManager` for `QuartzDeadlineManager` so deadlines survive
  restarts and work across instances.
- Add aggregate snapshotting to cap event-replay time for long-lived
  `BookingAggregate` instances.
- Formalise the dead-letter topic retry policy — current `DefaultErrorHandler`
  with `ExponentialBackOff` retries in-process; a proper DLT consumer would
  allow offline inspection and selective replay.
- Publish Pact contracts to a Pact Broker instead of committing them to the repo,
  enabling cross-team contract versioning.
