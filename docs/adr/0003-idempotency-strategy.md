# ADR 0003 — Idempotency strategy for inbound events

- Status: Accepted
- Date: 2026-07

## Context

Kafka provides **at-least-once** delivery. A consumer can see the same event more
than once: rebalance after a crash before the offset commits, a retry, or a
producer resend. For a payment, processing a `BookingConfirmed` twice means
**charging the customer twice** — unacceptable.

## Decision

Every integration event carries a unique `eventId` (a UUID minted by the
publisher). The critical consumer — `payment-service` — performs an atomic
**Redis `SET eventId 1 NX EX 24h`** before acting:

- If the key was **absent** (set succeeds), this is the first delivery → process.
- If the key **already exists**, this is a duplicate → skip.

Implemented in `IdempotencyGuard.firstDelivery(eventId)` using
`opsForValue().setIfAbsent(key, "1", ttl)`.

## Why Redis `SETNX`

- **Atomic** check-and-set in one round trip; no read-then-write race.
- **Fast** and out of the transactional path of the domain database.
- **TTL-bounded** memory: 24h comfortably exceeds Kafka's redelivery window while
  keeping the dedup set from growing unbounded.

## Defence in depth

Redis idempotency is the first line, but not the only one:

- The **aggregate state guard** is a second line: `MarkBookingPaid` on an already
  `PAID` booking is rejected, so even a duplicate that slips past Redis cannot
  double-transition the booking.
- Kafka message **keys** (`bookingId`) keep a booking's events on one partition,
  preserving order and making per-key reasoning valid.

## Failure mode: fail-open on Redis outage

If Redis is unavailable, `IdempotencyGuard.firstDelivery()` catches the
exception, logs the error, and **returns `true` (allow processing)**. This is a
deliberate fail-open choice:

- **Dropping a payment event is worse than a potential duplicate.** A lost
  payment is silent and hard to diagnose; a duplicate charge is caught by the
  aggregate's second line of defence (see below) and surfaced in logs/metrics.
- The aggregate state guard makes the duplicate survivable: `MarkBookingPaid` on
  an already-`PAID` booking is rejected with an `IllegalStateException`, so the
  duplicate command results in a logged rejection, not a double transition.
- Fail-closed (returning `false`) would silently route every event to the DLT
  during a Redis outage, stalling all payments with no user-visible error until
  manual DLT replay.

The trade-off: genuine first-deliveries processed during a Redis outage are not
recorded in the dedup store, so if Kafka redelivers them after Redis recovers,
the duplicate might slip through. In practice this window is narrow and the
aggregate guard provides a safety net.

## Consequences

- Redis is on the critical path for deduplication but not for payment liveness;
  a Redis outage degrades dedup guarantees rather than halting payment processing.
- The guard dedups on `eventId`, not business key, so a genuinely new event for
  the same booking is still processed.

## Alternatives considered

- **Database unique constraint on `eventId`:** works, but couples dedup to the
  domain transaction and is slower; Redis keeps it cheap and separable.
- **Kafka exactly-once semantics (EOS/transactions):** covers Kafka-to-Kafka, not
  the side effect of charging a card; insufficient alone.
