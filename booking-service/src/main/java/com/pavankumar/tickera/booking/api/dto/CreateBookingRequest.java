package com.pavankumar.tickera.booking.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateBookingRequest(
        @NotBlank String customerId,
        @NotBlank String eventName,
        @Min(1) int seats,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency) {
}
