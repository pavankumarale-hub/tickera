# ADR 0004 — Eventual consistency and the booking saga

- Status: Accepted
- Date: 2026-07

## Context

With database-per-service and Kafka between services, there is no distributed
transaction spanning "confirm booking" and "take payment". The booking is
confirmed in one service; the charge happens in another, asynchronously. We need
a defined way to reach a consistent end state — including when the reply is late
or never comes.

## Decision

Use an **Axon Saga** (`BookingSaga`) as the orchestrator for the booking→payment
process, and accept **eventual consistency** as the model between services.

The saga:

1. Starts on `BookingConfirmedEvent` and schedules a **payment deadline**.
2. Ends on `BookingPaidEvent` (success) — cancelling the deadline.
3. Ends on `BookingCancelledEvent`.
4. On deadline expiry, issues the **compensating** `CancelBookingCommand`
   ("payment not received in time"), releasing the held seats.

Payment outcomes arrive over Kafka; `PaymentEventsConsumer` translates them into
`MarkBookingPaid` / `CancelBooking` commands, which drive the events the saga
reacts to.

## Why a saga (not a chain of listeners)

- **Someone must own the timeout.** The hardest part of eventual consistency is
  "what if the response never arrives." A saga makes that an explicit,
  testable state with a deadline handler — a plain listener chain silently leaks
  bookings stuck in `CONFIRMED` forever.
- **Compensation over rollback.** We cannot roll back across services, so we
  compensate: cancel the booking to undo the reservation. The saga is the natural
  home for that logic.
- **Correlation.** Axon associates saga instances by `bookingId`, so concurrent
  bookings each get their own lifecycle without manual correlation code.

## Consistency guarantees

- **Within a service:** strong/immediate — the aggregate + event store are one
  transaction.
- **Across services:** eventual — a booking is `CONFIRMED` for a short window
  before it becomes `PAID` or `CANCELLED`. Clients observe this via the read
  model and are expected to treat `CONFIRMED` as "in progress."

## Consequences

- Read models can be briefly stale; the API surfaces intermediate states
  (`CREATED`, `CONFIRMED`) rather than hiding them.
- The demo uses `SimpleDeadlineManager` (in-memory). For production, swap in
  `QuartzDeadlineManager` so deadlines survive restarts and coordinate across
  instances (tracked in the booking LLD "next steps").
- Compensation is business-visible (a cancellation), which is correct: undoing a
  reservation is a real event, not a hidden rollback.
