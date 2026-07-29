package com.pavankumar.tickera.booking.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateBookingRequest(
        @NotBlank @Size(max = 100) String customerId,
        @NotBlank @Size(max = 200) String eventName,
        @Min(1) @Max(500) int seats,
        @NotNull @DecimalMin(value = "0.01") @DecimalMax(value = "9999999.99") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency) {
}
