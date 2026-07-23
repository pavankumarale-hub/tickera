# Tickera — Event-Driven Ticket Booking

[![CI](https://github.com/pavankumarale-hub/tickera/actions/workflows/ci.yml/badge.svg)](https://github.com/pavankumarale-hub/tickera/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen)
![Axon](https://img.shields.io/badge/Axon-4.9-purple)
![Kafka](https://img.shields.io/badge/Kafka-event_backbone-black)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

A public, production-shaped microservices system for booking event tickets, built to
demonstrate senior distributed-systems patterns: **CQRS + event sourcing (Axon)**
inside each service, **Kafka** as the integration backbone between services, a
**Saga** for cross-service orchestration with timeout and compensation, **Redis** for
exactly-once idempotency and read-through caching, **Pact consumer-driven contract
testing** on an asynchronous Kafka boundary, and **Micrometer → Prometheus → Grafana**
observability.

> One command to run everything: `docker compose up --build` → then `make demo`.

---

## Architecture at a glance

```mermaid
flowchart LR
    client([Client])

    subgraph booking[booking-service · Axon CQRS/ES + Saga]
        direction TB
        bAgg[BookingAggregate]
        bProj[(Read model · Postgres)]
        bSaga[BookingSaga\ntimeout + compensation]
    end
    subgraph payment[payment-service · Axon CQRS/ES]
        direction TB
        pAgg[PaymentAggregate]
        idem[[Redis idempotency]]
    end
    subgraph notif[notification-service · plain Kafka consumer]
        nDb[(Notifications · Postgres)]
    end

    redis[[Redis cache]]
    kafka{{Kafka}}

    client -->|REST| booking
    client -->|REST| payment
    client -->|REST| notif
    bProj -. read-through .- redis
    booking -->|BookingConfirmed| kafka
    kafka --> payment
    kafka --> notif
    payment -->|PaymentCompleted / Failed| kafka
    kafka --> booking
```

Full write-path diagram, saga sequence, and design rationale are in
[`docs/architecture.md`](docs/architecture.md).

### The happy path, in one sentence

`POST /bookings` → confirm emits **BookingConfirmed** to Kafka →
payment-service charges (idempotently) and emits **PaymentCompleted** →
the booking saga marks the booking **PAID**. If payment never arrives within
15 minutes, the saga's **deadline** fires a compensating `CancelBooking`. An
amount > $1 000 is declined, driving the booking to **CANCELLED** immediately.

## Why this exists

Most portfolios show CRUD over REST. This one shows the harder, more senior
parts: event sourcing, sagas, eventual consistency, idempotency under
at-least-once delivery, and contract testing on message boundaries — with the
reasoning written down as [Architecture Decision Records](docs/adr) and a
[low-level design doc](docs/LLD-booking-service.md), the way these decisions
are documented on real projects.

## Services

| Service | Port | Stack highlights | Swagger |
|---------|------|------------------|---------|
| `booking-service` | 8081 | Axon aggregate + projection + **saga**, Flyway, Kafka pub/sub, Redis cache, DLT error handler | http://localhost:8081/swagger-ui.html |
| `payment-service` | 8082 | Axon aggregate, **Redis `SETNX` idempotency**, Flyway, Kafka pub/sub | http://localhost:8082/swagger-ui.html |
| `notification-service` | 8083 | Non-Axon Spring Kafka consumer, Flyway, Postgres | http://localhost:8083/swagger-ui.html |
| `common-events` | — | Shared Kafka integration-event contracts | — |

Infra: Kafka + Zookeeper · Redis · one Postgres **per service** ·
Prometheus (`:9090`) · Grafana (`:3000`, anonymous viewer).

## Quick start

**Prerequisites:** Docker with Compose. JDK/Maven are not required to run the
stack — images build inside Docker. To build or run tests locally you need
JDK 17 (the bundled `./mvnw` fetches Maven automatically).

```bash
# 1. Start the full stack (Kafka, Redis, 3× Postgres, 3 services, Prometheus, Grafana)
docker compose up --build

# 2. In another terminal — run the happy-path demo
make demo

# 3. Or drive the failure/compensation path (amount > $1 000 → DECLINED → CANCELLED)
make demo-fail
```

### Try it by hand

```bash
# Create a booking
BID=$(curl -s -X POST localhost:8081/api/v1/bookings \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"cust-42","eventName":"Symphony Gala","seats":3,"amount":240.00,"currency":"USD"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["bookingId"])')

# Confirm — starts the saga and publishes to Kafka
curl -s -X POST localhost:8081/api/v1/bookings/$BID/confirm >/dev/null

# Poll until settled
curl -s localhost:8081/api/v1/bookings/$BID | python3 -m json.tool
curl -s "localhost:8082/api/v1/payments?bookingId=$BID" | python3 -m json.tool
curl -s "localhost:8083/api/v1/notifications?bookingId=$BID" | python3 -m json.tool
```

## Testing strategy

Tests are split into three JUnit 5 tag groups that CI runs as separate jobs:

| Tag | What it proves | Key classes |
|-----|----------------|-------------|
| `unit` | Aggregate invariants + state transitions via Axon `given/when/then`, no Spring or infra | `BookingAggregateTest`, `PaymentAggregateTest`, `NotificationListenerTest` |
| `contract` | booking-service and payment-service agree on the shape of the `BookingConfirmed` Kafka message (Pact V3 async CDC) | `BookingEventsConsumerPactTest` (generates pact) · `BookingEventsProviderPactTest` (verifies) |
| `integration` | Real Kafka + Postgres via Testcontainers: command→event→projection and the integration event actually lands on the topic | `BookingFlowIntegrationTest` |

```bash
make test-unit          # fast — no Docker needed
make test-contract      # generates pact then verifies it
make test-integration   # Testcontainers (needs Docker)
make test               # all three in one pass
```

The CI pipeline mirrors this structure: three parallel jobs (`unit-tests`,
`contract-tests`, `integration-tests`) fan out after a compile gate, then
`sonar` gathers the results.

## Observability

Every service exposes `/actuator/prometheus`. Prometheus scrapes all three;
Grafana auto-provisions the **Tickera — Service Overview** dashboard.

**Dashboard rows:**

| Row | Panels |
|-----|--------|
| HTTP Traffic | Request rate (req/s) · p95 latency (s) · 5xx error rate |
| Booking Funnel & Payment | Booking state transitions (CREATED → CONFIRMED → PAID / CANCELLED) · Redis idempotency guard (first-delivery vs. duplicates blocked) |
| Infrastructure | Kafka consumer record rate · JVM heap · JVM GC pause rate |

The `$service` variable filters panels to a single microservice or shows all three.

- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090

## Design docs

- [Architecture & diagrams](docs/architecture.md) — write-path flow, saga sequence diagram, technology map
- [Low-level design — booking-service](docs/LLD-booking-service.md)
- ADRs:
  [Axon vs plain Kafka](docs/adr/0001-axon-over-plain-kafka.md) ·
  [Database per service](docs/adr/0002-database-per-service.md) ·
  [Idempotency](docs/adr/0003-idempotency-strategy.md) ·
  [Eventual consistency & sagas](docs/adr/0004-eventual-consistency-and-sagas.md)

## Repository layout

```
tickera/
├── common-events/          # shared Kafka integration-event contracts (eventId, bookingId, amount …)
├── booking-service/        # Axon CQRS/ES, saga, Flyway, DLT error handler, Redis cache
├── payment-service/        # Axon CQRS/ES, Redis SETNX idempotency, Flyway
├── notification-service/   # plain Spring Kafka consumer, Flyway
├── docs/                   # architecture.md, LLD, ADRs
├── infra/                  # Prometheus config, Grafana provisioning + dashboard JSON
├── pacts/                  # generated Pact consumer contracts (committed, read by provider CI job)
├── scripts/demo.sh         # end-to-end demo (happy path + failure/compensation path)
├── Makefile                # build / test / demo shortcuts
├── docker-compose.yml      # one-command full stack
└── .github/workflows/ci.yml  # build → unit → contract ∥ integration → sonar
```

## How it was built (commit history)

Built incrementally to show real engineering progression rather than a single
large dump:

1. Scaffold three services: OpenAPI specs, Dockerfiles, Maven multi-module, CI skeleton.
2. Axon command/event/projection lifecycle + `BookingAggregate` state machine.
3. `payment-service` + Kafka event bridge + `BookingSaga` with deadline compensation.
4. Flyway migrations (all Axon tables version-controlled), Kafka dead-letter topics, `DefaultErrorHandler` with `ExponentialBackOff`.
5. Pact V3 async CDC contract tests + Testcontainers integration test; JUnit 5 tag strategy; multi-job CI pipeline.
6. Micrometer booking-funnel counters, Redis idempotency metrics, `RedisCacheManagerBuilderCustomizer`, Grafana dashboard v2.

## Next steps

- `QuartzDeadlineManager` — replace the in-memory `SimpleDeadlineManager` so saga deadlines survive a service restart.
- Aggregate snapshotting — cap event replay time for long-lived `BookingAggregate` instances.
- Pact Broker — publish and version pact files centrally instead of committing them to the repo.

## License

MIT — see [LICENSE](LICENSE).
