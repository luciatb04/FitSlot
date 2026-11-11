package org.springframework.samples.fitslot.access;

import org.springframework.samples.fitslot.membership.ServiceType;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
    @NotNull Integer memberId,
    @NotNull ServiceType serviceAccessed
) {
}
