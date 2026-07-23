package com.pavankumar.tickera.booking.api;

import com.pavankumar.tickera.booking.api.dto.BookingResponse;
import com.pavankumar.tickera.booking.api.dto.CancelBookingRequest;
import com.pavankumar.tickera.booking.api.dto.CreateBookingRequest;
import com.pavankumar.tickera.booking.coreapi.BookingStatus;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.CancelBookingCommand;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.ConfirmBookingCommand;
import com.pavankumar.tickera.booking.coreapi.commands.BookingCommands.CreateBookingCommand;
import com.pavankumar.tickera.booking.query.BookingQueryService;
import com.pavankumar.tickera.booking.query.BookingSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Command/query REST surface for bookings. Writes go through the Axon
 * {@link CommandGateway} (fire the command, wait for the aggregate to accept it);
 * reads go through {@link BookingQueryService} against the projection. The full
 * contract lives in {@code src/main/resources/openapi/booking-api.yaml}.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Create and track ticket bookings")
public class BookingController {

    private final CommandGateway commandGateway;
    private final BookingQueryService queryService;

    public BookingController(CommandGateway commandGateway, BookingQueryService queryService) {
        this.commandGateway = commandGateway;
        this.queryService = queryService;
    }

    @PostMapping
    @Operation(summary = "Create a booking in CREATED state")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        String bookingId = UUID.randomUUID().toString();
        commandGateway.sendAndWait(new CreateBookingCommand(
                bookingId, request.customerId(), request.eventName(),
                request.seats(), request.amount(), request.currency()));
        // Return the known state directly rather than querying the projection:
        // the TrackingEventProcessor is async, so the read model may not have
        // processed BookingCreatedEvent yet when sendAndWait returns.
        return ResponseEntity
                .created(URI.create("/api/v1/bookings/" + bookingId))
                .body(new BookingResponse(bookingId, request.customerId(), request.eventName(),
                        request.seats(), request.amount(), request.currency(),
                        BookingStatus.CREATED, null, null));
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Reserve seats and emit BookingConfirmed (starts the payment saga)")
    public ResponseEntity<BookingResponse> confirm(@PathVariable String bookingId) {
        commandGateway.sendAndWait(new ConfirmBookingCommand(bookingId));
        return ResponseEntity.accepted().body(BookingResponse.from(requireBooking(bookingId)));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel a booking that has not yet been paid")
    public ResponseEntity<BookingResponse> cancel(@PathVariable String bookingId,
                                                  @Valid @RequestBody CancelBookingRequest request) {
        commandGateway.sendAndWait(new CancelBookingCommand(bookingId, request.reason()));
        return ResponseEntity.ok(BookingResponse.from(requireBooking(bookingId)));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Fetch a single booking (Redis read-through cache)")
    public BookingResponse get(@PathVariable String bookingId) {
        return BookingResponse.from(requireBooking(bookingId));
    }

    @GetMapping
    @Operation(summary = "List bookings, optionally filtered by customer")
    public List<BookingResponse> list(@RequestParam(required = false) String customerId) {
        List<BookingSummary> results = (customerId == null)
                ? queryService.findAll()
                : queryService.findByCustomer(customerId);
        return results.stream().map(BookingResponse::from).toList();
    }

    private BookingSummary requireBooking(String bookingId) {
        BookingSummary summary = queryService.findById(bookingId);
        if (summary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown booking " + bookingId);
        }
        return summary;
    }
}
