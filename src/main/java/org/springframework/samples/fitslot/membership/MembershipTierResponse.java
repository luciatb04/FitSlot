package org.springframework.samples.fitslot.membership;

import java.math.BigDecimal;
import java.util.Set;

public record MembershipTierResponse(
    Integer id,
    String name,
    BigDecimal price,
    Integer durationDays,
    Integer entryQuota,
    Set<ServiceType> allowedServices,
    TimeBand timeBand
) {
}
