package com.pavankumar.tickera.payment.coreapi;

/**
 * Terminal payment outcomes. A payment settles exactly once: either
 * {@code COMPLETED} (charge authorised within the stub limit) or
 * {@code DECLINED} (rejected). Transitions are enforced inside
 * {@code PaymentAggregate} and reflected in the {@code PaymentSummary}
 * read model by {@code PaymentProjection}.
 */
public enum PaymentStatus {
    COMPLETED,
    DECLINED
}
