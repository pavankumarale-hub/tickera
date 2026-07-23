package com.pavankumar.tickera.booking.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Enables Spring's caching abstraction and tunes the Redis-backed cache manager.
 *
 * <p>TTL is declared here in code (not in YAML) so it carries a proper type
 * ({@link Duration}) and is verified at compile time. Statistics are enabled so
 * Spring Boot Actuator can expose cache hit/miss ratios to Micrometer → Prometheus.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        RedisCacheConfiguration bookingsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        return builder -> builder
                .enableStatistics()
                .withCacheConfiguration("bookings", bookingsConfig);
    }
}
