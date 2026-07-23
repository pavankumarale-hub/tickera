package com.pavankumar.tickethub.booking.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's caching abstraction. The cache manager itself is the
 * Redis-backed one auto-configured from {@code spring.cache.type=redis} in
 * {@code application.yml}.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
