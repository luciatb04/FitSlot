package org.springframework.samples.fitslot.payment;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
    @NotNull Integer memberId,
    @NotNull Integer tierId,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @NotNull PaymentStatus status
) {
}
