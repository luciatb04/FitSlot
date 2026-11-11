
package org.springframework.samples.fitslot.membership.TimeBandConfiguration;

import java.time.LocalTime;
import java.util.Set;

import org.springframework.samples.fitslot.membership.ServiceType;

public record MembershipConfigurationDTO(
    Set<ServiceType> allowedServices,
    TimeBand timeBand,
    LocalTime startTime,
    LocalTime endTime
) {}
