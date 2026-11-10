package org.springframework.samples.fitslot.membership;

import java.math.BigDecimal;
import java.util.Set;


/*
Define un record para representar la respuesta de un plan de membresía, incluyendo 
su ID, nombre, precio, duración, cuota de entradas, servicios permitidos y 
banda horaria.
*/
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