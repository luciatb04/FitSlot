package org.springframework.samples.fitslot.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.samples.fitslot.membership.SubscriptionStatus;

public record PaymentRecordResponse(
    Integer id,
    Integer memberId,
    String memberName,
    Integer subscriptionId,
    Integer tierId,
    String tierName,
    BigDecimal amount,
    LocalDateTime paymentDate,
    PaymentStatus status,
    SubscriptionStatus subscriptionStatus,
    LocalDate startDate,
    LocalDate endDate,
    Integer remainingEntries
) {
}
