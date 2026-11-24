package org.springframework.samples.fitslot.classes;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClassSessionRequest(
        @NotBlank String name,
        @NotNull Room room,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Min(1) Integer maxCapacity,
        @NotNull @Min(1) Integer gymId) {
}
