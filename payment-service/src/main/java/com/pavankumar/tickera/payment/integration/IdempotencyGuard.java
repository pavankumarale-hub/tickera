package com.pavankumar.tickera.payment.integration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed exactly-once guard for inbound Kafka events.
 *
 * <p>Kafka gives at-least-once delivery, so the same {@code BookingConfirmed}
 * event can arrive twice (consumer restart, rebalance, retry). Before we charge
 * a card we do an atomic {@code SET key value NX EX ttl}: the first arrival wins
 * and proceeds, any redelivery finds the key already present and is dropped.
 *
 * <p>The TTL bounds memory while comfortably outliving Kafka's redelivery window.
 * See {@code docs/adr/0003-idempotency-strategy.md}.
 */
@Component
public class IdempotencyGuard {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "payment:processed-event:";

    private final StringRedisTemplate redis;
    private final Counter firstDeliveryCounter;
    private final Counter duplicateCounter;

    public IdempotencyGuard(StringRedisTemplate redis, MeterRegistry registry) {
        this.redis = redis;
        this.firstDeliveryCounter = Counter.builder("tickera.payment.idempotency")
                .tag("result", "first_delivery")
                .description("Kafka events processed for the first time")
                .register(registry);
        this.duplicateCounter = Counter.builder("tickera.payment.idempotency")
                .tag("result", "duplicate")
                .description("Kafka redeliveries blocked by the idempotency guard")
                .register(registry);
    }

    /**
     * @return {@code true} if this eventId has not been seen before (caller should
     *         process it); {@code false} if it is a duplicate (caller should skip).
     */
    public boolean firstDelivery(String eventId) {
        Boolean set = redis.opsForValue()
                .setIfAbsent(KEY_PREFIX + eventId, "1", TTL);
        boolean isFirst = Boolean.TRUE.equals(set);
        if (isFirst) {
            firstDeliveryCounter.increment();
        } else {
            duplicateCounter.increment();
        }
        return isFirst;
    }
}
