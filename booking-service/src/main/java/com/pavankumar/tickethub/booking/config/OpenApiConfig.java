package com.pavankumar.tickethub.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("TicketHub Booking API")
                .version("v1")
                .description("Command/query API for the event-sourced booking lifecycle.")
                .license(new License().name("MIT")));
    }
}
