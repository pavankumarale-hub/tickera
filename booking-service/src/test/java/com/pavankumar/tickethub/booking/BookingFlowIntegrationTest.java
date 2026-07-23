package com.pavankumar.tickethub.booking;

import com.pavankumar.tickethub.booking.api.dto.BookingResponse;
import com.pavankumar.tickethub.booking.api.dto.CreateBookingRequest;
import com.pavankumar.tickethub.common.events.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end slice on real infrastructure: a Postgres event store and a Kafka
 * broker, both in throwaway containers. Proves the full write path
 * (command → event → projection) <em>and</em> that confirming a booking actually
 * publishes the integration event other services depend on. This is the test
 * that would run in CI to catch wiring regressions a unit test can't.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cache.type=simple",          // no Redis needed for this slice
                "spring.data.redis.repositories.enabled=false"
        })
class BookingFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("booking_db");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.1"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void confirmingABooking_updatesReadModel_andPublishesIntegrationEvent() {
        CreateBookingRequest request =
                new CreateBookingRequest("cust-42", "Symphony Gala", 3, new BigDecimal("240.00"), "USD");

        BookingResponse created = rest.postForObject("/api/v1/bookings", request, BookingResponse.class);
        assertThat(created).isNotNull();
        assertThat(created.status().name()).isEqualTo("CREATED");
        String id = created.bookingId();

        try (KafkaConsumer<String, String> consumer = bookingEventsConsumer()) {
            rest.postForObject("/api/v1/bookings/{id}/confirm", null, BookingResponse.class, id);

            // read model catches up (projection is eventually consistent)
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                BookingResponse fetched =
                        rest.getForObject("/api/v1/bookings/{id}", BookingResponse.class, id);
                assertThat(fetched.status().name()).isEqualTo("CONFIRMED");
            });

            // integration event reached Kafka
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                List<String> keys = new java.util.ArrayList<>();
                for (ConsumerRecord<String, String> r : records) {
                    keys.add(r.key());
                }
                assertThat(keys).contains(id);
            });
        }
    }

    private KafkaConsumer<String, String> bookingEventsConsumer() {
        Properties props = new Properties();
        props.putAll(Map.of(
                "bootstrap.servers", KAFKA.getBootstrapServers(),
                "group.id", "integration-test",
                "auto.offset.reset", "earliest",
                "key.deserializer", StringDeserializer.class.getName(),
                "value.deserializer", StringDeserializer.class.getName()));
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(KafkaTopics.BOOKING_EVENTS));
        return consumer;
    }
}
