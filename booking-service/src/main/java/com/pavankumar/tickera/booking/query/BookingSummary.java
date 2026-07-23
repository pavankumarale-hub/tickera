package com.pavankumar.tickera.booking.query;

import com.pavankumar.tickera.booking.coreapi.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Denormalised read model (the "Q" in CQRS). Built purely by replaying events in
 * {@link BookingProjection}; the REST query API only ever reads this table, never
 * the event store. Implements {@link Serializable} so instances can be cached in
 * Redis.
 */
@Entity
@Table(name = "booking_summary")
public class BookingSummary implements Serializable {

    @Id
    private String bookingId;
    private String customerId;
    private String eventName;
    private int seats;
    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "payment_id")
    private String paymentId;

    private Instant updatedAt;

    public BookingSummary() {
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
