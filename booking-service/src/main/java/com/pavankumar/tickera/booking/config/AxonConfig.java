package com.pavankumar.tickera.booking.config;

import org.axonframework.config.Configuration;
import org.axonframework.config.ConfigurationScopeAwareProvider;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.SimpleDeadlineManager;
import org.springframework.context.annotation.Bean;

/**
 * Wires the {@link DeadlineManager} the {@code BookingSaga} uses for its payment
 * timeout. {@link SimpleDeadlineManager} keeps schedules in memory — fine for a
 * single-node demo; swap in {@code QuartzDeadlineManager} for durable, multi-node
 * deadlines (noted in ADR-0004).
 */
@org.springframework.context.annotation.Configuration
public class AxonConfig {

    @Bean
    public DeadlineManager deadlineManager(Configuration configuration) {
        return SimpleDeadlineManager.builder()
                .scopeAwareProvider(new ConfigurationScopeAwareProvider(configuration))
                .build();
    }
}
