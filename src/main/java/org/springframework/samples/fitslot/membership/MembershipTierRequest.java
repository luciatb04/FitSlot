package org.springframework.samples.fitslot.membership;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/*
Recibe los campos de un plan de membresia y los valida, para luego ser usado en el controlador
*/

public record MembershipTierRequest(
    @NotBlank String name,
    @NotNull @DecimalMin("0.00") BigDecimal price,
    @Positive Integer durationDays,
    @Positive Integer entryQuota,
    @NotEmpty Set<@NotNull ServiceType> allowedServices,
    @NotNull TimeBand timeBand
) {
}