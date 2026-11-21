
package org.springframework.samples.fitslot.classes;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record UpdateClassSessionRequest(

    @NotNull LocalDate date,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotNull Room room

) {}
