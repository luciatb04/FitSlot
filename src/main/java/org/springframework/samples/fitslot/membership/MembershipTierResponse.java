package org.springframework.samples.fitslot.membership;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;

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
