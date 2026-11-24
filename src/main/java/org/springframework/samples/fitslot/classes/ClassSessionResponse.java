
package org.springframework.samples.fitslot.classes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.samples.fitslot.instructor.Instructor;

public record ClassSessionResponse(
    Integer id,
    String name,
    Room room,
    LocalDate date,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Integer maxCapacity,
    long confirmedBookings,
    Integer instructorId,
    String instructorName,
    Integer availableSpots
) {
    public static ClassSessionResponse fromEntity(
            ClassSession session,
            long confirmedBookings) {

        int availableSpots = (int) Math.max(
                0L,
                (long) session.getMaxCapacity() - confirmedBookings);

        Instructor instructor = session.getInstructor();

        String instructorName = instructor == null
                ? null
                : instructor.getFirstName() + " "
                  + instructor.getLastName();

        return new ClassSessionResponse(
                session.getId(),
                session.getName(),
                session.getRoom(),
                session.getDate(),
                session.getDate().getDayOfWeek(),
                session.getStartTime(),
                session.getEndTime(),
                session.getMaxCapacity(),
                confirmedBookings,
                instructor == null ? null : instructor.getId(),
                instructorName,
                availableSpots
        );
    }
}
