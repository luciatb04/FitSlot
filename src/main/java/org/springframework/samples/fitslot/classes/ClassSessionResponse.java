package org.springframework.samples.fitslot.classes;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.samples.fitslot.instructor.Instructor;

public record ClassSessionResponse(
    Integer id,
    String name,
    Room room,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    Integer maxCapacity,
    Integer instructorId,
    String instructorName
) {
    public static ClassSessionResponse fromEntity(
            ClassSession session) {

        Instructor instructor = session.getInstructor();

        String instructorName = instructor == null
            ? null
            : instructor.getFirstName() + " " +
              instructor.getLastName();

        return new ClassSessionResponse(
            session.getId(),
            session.getName(),
            session.getRoom(),
            session.getDate(),
            session.getStartTime(),
            session.getEndTime(),
            session.getMaxCapacity(),
            instructor == null ? null : instructor.getId(),
            instructorName
        );
    }
}
