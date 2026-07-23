package com.pavankumar.tickethub.booking.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelBookingRequest(@NotBlank String reason) {
}
