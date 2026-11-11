package org.springframework.samples.fitslot.membership;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MembershipTierRequest(
    @NotBlank String name,
    @NotNull @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    BigDecimal price,
    @Positive Integer durationDays,
    @Positive Integer entryQuota,
    @NotEmpty Set<@NotNull ServiceType> allowedServices,
    @NotNull TimeBand timeBand
) {
    @AssertTrue(message = "A membership tier must define either durationDays or entryQuota, but not both")
    public boolean hasExactlyOnePlanType() {
        return (durationDays == null) != (entryQuota == null);
    }
}
