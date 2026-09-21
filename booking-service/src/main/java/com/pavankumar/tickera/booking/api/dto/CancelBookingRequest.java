package com.pavankumar.tickera.booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for the cancel-booking endpoint. The {@code reason} is stored on
 * the resulting {@code BookingCancelledEvent} and surfaced in the read model for
 * audit purposes. Bean Validation rejects blank or overly long values before the
 * command is dispatched.
 */
public record CancelBookingRequest(@NotBlank @Size(max = 500) String reason) {
}
