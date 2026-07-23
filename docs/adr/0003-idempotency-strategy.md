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

## Consequences

- Redis is now on the critical path for payment; its availability matters. A
  Redis outage should fail closed (do not process) rather than risk double
  charges — noted as a hardening item.
- The guard dedups on `eventId`, not business key, so a genuinely new event for
  the same booking is still processed.

## Alternatives considered

- **Database unique constraint on `eventId`:** works, but couples dedup to the
  domain transaction and is slower; Redis keeps it cheap and separable.
- **Kafka exactly-once semantics (EOS/transactions):** covers Kafka-to-Kafka, not
  the side effect of charging a card; insufficient alone.
