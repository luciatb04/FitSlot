
package org.springframework.samples.fitslot.classes.InstructorSubstitution;

import org.springframework.samples.fitslot.instructor.Instructor;

public record AvailableInstructorResponse(
    Integer id,
    String firstName,
    String lastName
) {
    public static AvailableInstructorResponse fromEntity(
            Instructor instructor) {

        return new AvailableInstructorResponse(
            instructor.getId(),
            instructor.getFirstName(),
            instructor.getLastName()
        );
    }
}
