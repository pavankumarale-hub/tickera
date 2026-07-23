package com.pavankumar.tickera.payment.api;

import com.pavankumar.tickera.payment.api.dto.PaymentResponse;
import com.pavankumar.tickera.payment.query.PaymentSummary;
import com.pavankumar.tickera.payment.query.PaymentSummaryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Read-only view over the payment read model. Payments are never created
 * through HTTP here — they are triggered by the {@code BookingConfirmed} Kafka
 * event — which is exactly the point of an event-driven design: the write path
 * is a reaction, the REST surface is for querying.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Inspect payment outcomes")
public class PaymentController {

    private final PaymentSummaryRepository repository;

    public PaymentController(PaymentSummaryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get a payment by id")
    public PaymentResponse get(@PathVariable String paymentId) {
        PaymentSummary summary = repository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown payment " + paymentId));
        return PaymentResponse.from(summary);
    }

    @GetMapping
    @Operation(summary = "List payments, optionally filtered by booking")
    public List<PaymentResponse> list(@RequestParam(required = false) String bookingId) {
        List<PaymentSummary> results = (bookingId == null)
                ? repository.findAll()
                : repository.findByBookingId(bookingId);
        return results.stream().map(PaymentResponse::from).toList();
    }
}
