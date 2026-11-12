package org.springframework.samples.fitslot.payment;

import jakarta.validation.constraints.NotNull;

public record PaymentStatusRequest(@NotNull PaymentStatus status) {
}
