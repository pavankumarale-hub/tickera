package com.pavankumar.tickera.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the OpenAPI metadata shown in the Swagger UI for this service.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Tickera Payment API")
                .version("v1")
                .description("Read model over payment outcomes (writes are event-driven via Kafka).")
                .license(new License().name("MIT")));
    }
}
