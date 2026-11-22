package org.springframework.samples.fitslot.instructor;

import java.time.LocalDate;
import java.time.LocalTime;

public record InstructorAvailabilityResponse(
    Integer id,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime
) {
    public static InstructorAvailabilityResponse fromEntity(
            InstructorAvailability availability) {

        return new InstructorAvailabilityResponse(
            availability.getId(),
            availability.getDate(),
            availability.getStartTime(),
            availability.getEndTime()
        );
    }
}
